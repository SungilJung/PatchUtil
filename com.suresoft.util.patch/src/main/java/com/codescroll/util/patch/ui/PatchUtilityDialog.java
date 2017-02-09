package com.codescroll.util.patch.ui;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.codescroll.util.patch.Const;
import com.codescroll.util.patch.internal.CodeScrollExecutor;
import com.codescroll.util.patch.internal.Messages;
import com.codescroll.util.patch.internal.PatchExecutor;
import com.codescroll.util.patch.internal.PatchFileUtils;
import com.codescroll.util.patch.internal.PatchHistoryParser;
import com.codescroll.util.patch.model.PatchInfo;
import com.codescroll.util.patch.model.CSProduct;
import com.codescroll.util.patch.ui.composite.PatchContentsContainer;
import com.codescroll.util.patch.ui.dialog.PatchHistoryDialog;

public class PatchUtilityDialog extends TitleAreaDialog {

	/**	csc.exe */
	private final String CSC_EXE = "csc.exe";

	private static final int INDEX_PATCH_HISTORY_BUTTON = 0;
	private static final int INDEX_APPLAY_PATCH_BUTTON = 1;
	
	/**	외부 경로로부터 패치를 적용할 지 여부 */
	private final boolean EXTERNAL_MODE;
	{
		System.out.println("##### EXTERNAL MODE initialize #####");
		
		String externalPatchDirPath = PatchFileUtils.getExternalPatchDirPath();

		// 외부 패치 디렉터리가 존재하지 않거나, 패치 파일이 없다면
		if(externalPatchDirPath != null && 
				PatchExecutor.existPatchfiles(new File(externalPatchDirPath))) {
			EXTERNAL_MODE = true;
		} else {
			EXTERNAL_MODE = false;
		}
	}
	
	private enum ErrorCode {
		
		VALID_PATCH(0),
		EMPTY_INSTALL_PATH(-1),
		INVALID_INSTALL_PATH(-2),
		EMPTY_PATCH_DIR(-3),
		INVALID_PRODUCT(-4),
		EQUAL_PATCH_VERSION(-5),
		LOW_APPLY_PATCH_VERSION(-6),
		RUNNING_PRODUCT(-7),
		INVALID_RESTORE(-8);
		
		@SuppressWarnings("unused")
		private int errorCode;
		
		private ErrorCode(int errorCode) {
			this.errorCode = errorCode;
		}
	}
	
	/** 패치 내용 컨테이너 */
	private PatchContentsContainer patchContainer;
	/** 제품 설치 경로 텍스트 */
	private Text installPathText;
	/** 제품 설치 경로 찾는 버튼 */
	private Button productBrowseBtn;
	/**	설치된 제품 버전을 표시하는 레이블 */
	private Label CSProductVersionLabel;

	private PatchInfo patchInfoData;
	private String buttonNames[] = { Messages.PatchContentsComposite_ButtonA_0,
			Messages.PatchContentsComposite_ButtonB_0};

	/**	설치된 제품의 버전 */
	private String localProductVersion="";
	/** 설치된 제품명 */
	private	String localProductName="";
	/** 설치된 제품 패키지명 */
	private String productPackageName="";
	/** history.patch 파일의 경로 */
	private String historyPatchFilePath="";
	/** 마지막으로 적용된 패치 버전 */
	private String latestPatchedVersion = "";
	/** 백업 디렉터리 경로*/
	private String backupDirPath="";
	/**
	 * 생성자
	 * @param parentShell
	 */
	public PatchUtilityDialog(Shell parentShell) {
		super(parentShell);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(Const.UTILITY_NAME);

		InputStream in = ImageLoader.class.getClassLoader().getResourceAsStream(Const.CS_ICON);
		// 이미지가 없더라도 오류 없이 실행되도록 예외처리
		if(in != null) {
			Image img = new Image(null, in);
			newShell.setImage(img);
		}
	}

	@Override
	protected Control createDialogArea(final Composite parent) {
		System.out.println("call createDialogArea()");
		GridLayout gdLayout = new GridLayout();
		gdLayout.numColumns = Const.DEFAULT_COMPOSITE_COL_NUM;
		gdLayout.marginWidth = gdLayout.marginHeight = 10;

		Composite parentArea = (Composite) super.createDialogArea(parent);
		Composite container = new Composite(parentArea, SWT.NONE);

		container.setLayout(gdLayout);
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		createInstallPathArea(container);
		createProductVersionArea(container);

		patchContainer = new PatchContentsContainer(container, buttonNames);
		patchContainer.createPartControl();

		return parent;
	}


	/**
	 * 제품 설치 경로 부분 Composite
	 */
	private void createInstallPathArea(final Composite parent) {

		System.out.println("call createInstallPathArea()");
		Label installPathLabel = new Label(parent, SWT.NONE);
		installPathLabel.setText(Messages.CreateInstallPathArea_0);

		installPathText = new Text(parent, SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 400;
		installPathText.setLayoutData(gd);
		installPathText.setEditable(false);

		productBrowseBtn = new Button(parent, SWT.PUSH);
		gd = new GridData();
		gd.widthHint = 100;
		productBrowseBtn.setLayoutData(gd);
		productBrowseBtn.setText(Messages.CreateInstallPathArea_1);

	}

	/**
	 * CodeScroll 제품 버전 부분 Composite
	 */
	private void createProductVersionArea(Composite parent) {
		System.out.println("call createProductVersionArea()");
		Label productVersionLabel = new Label(parent, SWT.NONE);
		productVersionLabel.setText(Messages.CreateProductVersionArea);

		CSProductVersionLabel = new Label(parent, SWT.NONE);
		CSProductVersionLabel.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 2, 1));
	}

	@Override
	protected Control createContents(Composite parent) {
		System.out.println("call super createContents()");
		Control control = super.createContents(parent);
		System.out.println("call this createContents()");

		setTitle(Const.UTILITY_NAME);

		initInstallPathTextControl();
		initDialogContentsData();
		refreshButton();
		registerActionListener();
		initButtonFocus();

		return control;
	}


	/**
	 * 이전에 선택한 제품 경로 값이 있다면, <br>
	 * 패치할 대상 제품에 맞는 설치 경로로 초기화 
	 */
	private void initInstallPathTextControl() {

		System.out.println("call initInstallPathText()");

		File iniDir = new File(Const.INI_FILE_PATH);
		PatchInfo tempPatchInfo = null;
		
		if(EXTERNAL_MODE) {
			tempPatchInfo = loadPatchInfoDataFrom(PatchFileUtils.getExternalPatchDirPath(), "");
		} else {
			tempPatchInfo = loadPatchInfoDataFrom(PatchFileUtils.getJarFile(), "");
		}
		// 기본 설치 경로에 패치하려는 제품이 설치되어 있다면 초기화
		String productVersion = tempPatchInfo.getProductVersion();
		String installedDefaultPath = getInstalledDefaultPath(tempPatchInfo.getProductName(), productVersion);

		// 파일에 내용이 없거나, 파일의 내용에 해당하는 제품이 없는 경우 예외처리
		if(iniDir.exists()) {
			Map<String, String> installPathMap = PatchFileUtils.getInstallPathMap();

			if (!installPathMap.isEmpty()) {
				String version = PatchFileUtils.getProductMinorVersion(productVersion);
				String key = tempPatchInfo.getProductName() + "_" + version;
				String installPath = installPathMap.get(key);

				if(installPath != null) {
					installPathText.setText(installPath);
					
				}else {
					installPathText.setText(installedDefaultPath);
				}
				
			}else{
				installPathText.setText(installedDefaultPath);
			}

		} else if (!installedDefaultPath.isEmpty()) {
			installPathText.setText(installedDefaultPath);
		}
	}

	/**
	 * 패치할 제품이 기본 경로에 설치되어 있다면 설치 경로를 반환
	 * 
	 * @param productName
	 *            패치가 적용될 제품명
	 * @param productVersion
	 *            패치가 적용될 제품 버전
	 * 
	 * @return 제품 설치 경로
	 */
	private String getInstalledDefaultPath(final String productName, final String productVersion) {
		System.out.println("getInstalledDefaultPath()");

		String resultPath = "";
		String[] installedProductName;
		String x86_root_Dir = "C:\\Program Files (x86)";
		String x64_root_Dir = "C:\\Program Files";

		File path = new File(x86_root_Dir);
		resultPath = x86_root_Dir + File.separator;
		if (!path.exists()) {
			path = new File(x64_root_Dir);
			resultPath = x64_root_Dir + File.separator;
		}

		installedProductName = path.list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				int lastIndexOf = productVersion.lastIndexOf('.'); // $NON-NLS-1$
				String tempName = productName + " " + productVersion.substring(0, lastIndexOf); //$NON-NLS-1$

				int indexOf = name.indexOf(tempName);
				if (indexOf >= 0) {
					return true;
				}
				return false;
			}
		});

		if (installedProductName.length == 0) {
			return "";
		}

		return resultPath + installedProductName[0];
	}

	/**
	 * dialog의 contents 데이터를 초기화 한다.
	 */
	private void initDialogContentsData() {
		System.out.println("initDialogContentsData()");
		historyPatchFilePath = installPathText.getText() + File.separator + Const.BACKUP_ROOT_DIR_NAME + File.separator
				+ Const.PATCH_HISTORY_FILE_NAME;
		latestPatchedVersion = PatchHistoryParser.getLatestPatchedInfo(new File(historyPatchFilePath))
				.getPatchVersion();

		if (EXTERNAL_MODE) {
			patchInfoData = loadPatchInfoDataFrom(PatchFileUtils.getExternalPatchDirPath(), latestPatchedVersion);
		} else {
			patchInfoData = loadPatchInfoDataFrom(PatchFileUtils.getJarFile(), latestPatchedVersion);
		}
		backupDirPath = createBackupDirPath(latestPatchedVersion);
		CSProductVersionLabel.setText(getProductVersion());
		setViewerContents();
	}

	/**
	 * 
	 * Runnable Jar의 patch.info 파일에서 patchInfo 정보를 가져온다.
	 * 
	 * @param jarFile
	 *            패치 유틸리티 Jar 파일
	 * @param latestPatchedVersion
	 *            patch.history 파일로부터 읽어온 마지막 패치 버전. 없는 경우 빈 문자열
	 * @return
	 */
	private PatchInfo loadPatchInfoDataFrom(JarFile jarFile, String latestPatchedVersion) {

		InputStream inputStream = PatchExecutor.getInputStream(Const.PATCH_INFO_FILE_NAME, jarFile);
		PatchInfo patchInfo = PatchFileUtils.loadPatchInfo(inputStream, latestPatchedVersion);

		if (patchInfo == null) {
			MessageDialog.openError(getShell(), Messages.MessageDialogTitle_2, Messages.Dialog_Message_2);
			System.exit(0);
		}

		return patchInfo;
	}

	private PatchInfo loadPatchInfoDataFrom(String externalPath, String latestPatchedVersion) {

		InputStream inputStream = PatchExecutor.getInputStream(Const.PATCH_INFO_FILE_NAME, externalPath);
		PatchInfo patchInfo = PatchFileUtils.loadPatchInfo(inputStream, latestPatchedVersion);

		if (patchInfo == null) {
			MessageDialog.openError(getShell(), Messages.MessageDialogTitle_2, Messages.Dialog_Message_2);
			System.exit(0);
		}

		return patchInfo;
	}

	/**
	 * 패치 백업 경로를 생성한다.
	 * 
	 * @param patchVersion
	 *            마지막으로 적용된 패치 버전
	 */
	private String createBackupDirPath(String patchVersion) {

		String backupDirName = "";

		backupDirName = createBackupDirName(patchVersion);
		String backupDirPath = installPathText.getText() + File.separator + Const.BACKUP_ROOT_DIR_NAME + File.separator
				+ backupDirName;
		System.out.println("backup directory : " + backupDirPath);

		return backupDirPath;
	}

	private String createBackupDirName(String patchVersion) {
		if (patchVersion.isEmpty()) {
			return Const.FIRST_PATCH_BACKUP_DIR_NAME;
		} else {
			return patchVersion;
		}
	}

	/**
	 * 패치 내용 컨텐츠를 설정한다.
	 * <p>
	 * 
	 * 패치가 불가능한 에러 상황일 시에는 비활성화 처리
	 */
	private void setViewerContents() {
		System.out.println("### setViewerContents() ###");
		ErrorCode errCode = isValidPatch();
		System.out.println(
				"isValidPatch().equals(ErrorCode.VALID_PATCH): " + errCode.equals(ErrorCode.VALID_PATCH));
		
		if (errCode.equals(ErrorCode.VALID_PATCH)) {
			patchContainer.setEnabledText(true);
			patchContainer.setViewerContents(patchInfoData.toString());
			
		} else {
			patchContainer.setEnabledText(false);
		}
	}

	/**
	 * dialog의 control의 action에 대한 listener를 등록한다.
	 */
	private void registerActionListener() {
		registerInstallPathCompositeActionListner();
		registerPartCompositeSelectionAdapter(patchContainer, buttonNames);
	}

	private void registerInstallPathCompositeActionListner() {
		registerInstallPathModifyListner();
		registerFindInstallPathListener();
	}

	/**
	 * 설치경로 Text의 변경 액션에 대한 listener를 등록한다.
	 */
	private void registerInstallPathModifyListner() {
		installPathText.addModifyListener(new ModifyListener() {

			// 설치 경로가 변경될 때 제품 버전을 표시하는 레이블 및 패치 히스토리 경로를 변경한다.
			public void modifyText(ModifyEvent e) {
				String message = "";

				if (getMessage().isEmpty()) {
					message = getProductVersion();
				}
				CSProductVersionLabel.setText(message);
				initDialogContentsData();
			}
		});
	}

	/**
	 * 제품 설치 경로를 찾는 버튼 action의 listener를 등록한다.
	 */
	private void registerFindInstallPathListener() {
		productBrowseBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dialog = new DirectoryDialog(getShell());
				dialog.setText(Messages.CreateInstallPathArea_2);
				dialog.setFilterPath(installPathText.getText());
				String installPath = dialog.open();

				if (installPath != null) {
					// 설치경로가 맞지 않다면
					if (!isValidFileAndDirPath(installPath, CSC_EXE)) {
						setMessage(Messages.TitleMessage_1, IMessageProvider.ERROR);
					} else {
						// 적힌 메시지를 초기화
						setMessage(""); //$NON-NLS-1$
					}
					// 설치 경로 정상 여부와 상관없이 선택한 값을 텍스트에 설정해줌.
					installPathText.setText(installPath);
					refreshButton();
				}
			}
		});
	}

	private void registerPartCompositeSelectionAdapter(PatchContentsContainer patchContainer, String[] btNames) {
		patchContainer.registerButtonSelectionAdpater(btNames[INDEX_PATCH_HISTORY_BUTTON],
				createHistoryPathButtonSelectionAdatper());
		patchContainer.registerButtonSelectionAdpater(btNames[INDEX_APPLAY_PATCH_BUTTON],
				createApplayPatchButtonSelectionAdapter());

	}

	private SelectionAdapter createHistoryPathButtonSelectionAdatper() {
		SelectionAdapter selectionAdpater = null;

		selectionAdpater = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				PatchHistoryDialog patchHistoryDialog = new PatchHistoryDialog(getShell(), installPathText.getText());

				// 패치 이력 다이얼로그에서 되돌리기를 수행하면 제품을 실행시킨다.
				if (patchHistoryDialog.open() == OK) {
					CodeScrollExecutor.executeCodeScroll(installPathText.getText());
					close();
				}
			}
		};

		return selectionAdpater;
	}

	private SelectionAdapter createApplayPatchButtonSelectionAdapter() {
		SelectionAdapter selectionAdpater = null;

		selectionAdpater = new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				String productName = patchInfoData.getProductName();

				if (CodeScrollExecutor.isProductStarted(productName)) {
					boolean answer = MessageDialog.openConfirm(
							null, Messages.MessageDialogTitle_1, Messages.Dialog_Message_1);

					if(answer) {
						Map<String, String> startedPidMap = CodeScrollExecutor.getStartedProductsInfo();
						String pid = startedPidMap.get(productName);
						CodeScrollExecutor.processKill(pid);
					} else {
						// 이전 화면으로 돌아간다.
						return;
					}
				}

				final String installPath = installPathText.getText();

				final ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
				try {
					pmd.run(true, false, new IRunnableWithProgress() {

						public void run(final IProgressMonitor paramIProgressMonitor)
								throws InvocationTargetException, InterruptedException {
							// 백업 과 패치 적용을 해야하기에 * 2 를 추가함.
							int totalCount;
							if (EXTERNAL_MODE) {
								totalCount = PatchExecutor
										.getPatchFilePathList(PatchFileUtils.getExternalPatchDirPath()).size() * 2;
							} else {
								totalCount = PatchExecutor.getPatchFilePathList(PatchFileUtils.getJarFile()).size()
										* 2;
							}

							paramIProgressMonitor.beginTask(Messages.ProgressMessage_0, totalCount);
							final String productPluginsPath = installPath
									.concat(File.separator + Const.PLUGINS_DIR_NAME);
							final Set<String> patchFilePaths = createPatchFilePaths(productPluginsPath);
							/*
							 * 되돌리기 액션 수행 시, 신규로 추가된 파일을 제거하기 위해 패치 파일 리스트를
							 * 등록한다.
							 */
							patchInfoData.setPatchFilePathList(patchFilePaths);
							// 패치 유틸 INI 저장
							String version = PatchFileUtils.getProductMinorVersion(patchInfoData.getProductVersion());
							PatchFileUtils.saveInstallPath(new CSProduct(version,
									patchInfoData.getProductName(), installPath));

							// 0.패치를 적용할 파일들 백업
							copyFiles(productPluginsPath, backupDirPath, patchFilePaths, paramIProgressMonitor,
									Messages.Dialog_Message_0);

							// 1. 패치 적용
							boolean applyPatch;
							if (EXTERNAL_MODE) {
								applyPatch = PatchExecutor.applyPatch(productPluginsPath,
										PatchFileUtils.getExternalPatchDirPath(), paramIProgressMonitor);
							} else {
								applyPatch = PatchExecutor.applyPatch(productPluginsPath,
										PatchFileUtils.getJarFile(), paramIProgressMonitor);
							}
							if (!applyPatch) {
								// 패치 실패시 백업 데이터를 되돌리기 수행
								Display.getDefault().asyncExec(new Runnable() {

									public void run() {
										MessageDialog.openError(null, null, Messages.PatchFailMessage);
										copyFiles(backupDirPath, productPluginsPath, patchFilePaths,
												paramIProgressMonitor, Messages.Dialog_Message_8);
									}
								});

							} else {
								// 2-2. 패치 내역을 저장
								File patchHistory = new File(historyPatchFilePath);
								PatchHistoryParser.savePatchInfo(patchInfoData, patchHistory);
								// 3. 패치 적용 파일 백업, 패치 적용, 패치 유틸 ini와 패치 내역
								// 저장이 끝났으니 제품을 clean 옵션으로 실행 후 유틸을 종료한다.
								CodeScrollExecutor.executeCodeScroll(installPath);
							}
						}
					});
				} catch (InvocationTargetException e1) {
					e1.printStackTrace();
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}

				// 4. 유틸 종료
				close();
			}

			/**
			 * 패치 파일 경로를 생성한다.<br>
			 * 기존에 없는 신규 파일인 경우 파일 경로 앞에 "*"를 삽입한다.
			 * 
			 * @param pluginsDirPath
			 *            plugins 디렉터리 경로
			 * 
			 * @return 파일경로 set
			 */
			private Set<String> createPatchFilePaths(String pluginsDirPath) {

				Set<String> resultPatchFilePathList = new HashSet<String>();
				Set<String> patchFilePathList;
				if (EXTERNAL_MODE) {
					patchFilePathList = PatchExecutor.getPatchFilePathList(PatchFileUtils.getExternalPatchDirPath());
				} else {
					patchFilePathList = PatchExecutor.getPatchFilePathList(PatchFileUtils.getJarFile());
				}

				for (String patchFilePath : patchFilePathList) {
					File patchFile = new File(pluginsDirPath + File.separator + patchFilePath);
					// 기존에 없던 파일이 추가 되면 이름 앞에 +를 붙여 구분한다.
					if (!patchFile.exists()) {
						resultPatchFilePathList.add("*" + patchFilePath);
					} else {
						resultPatchFilePathList.add(patchFilePath);
					}
				}
				return resultPatchFilePathList;
			}

			/**
			 * 파일을 복사한다.
			 * 
			 * @param srcDir
			 * @param destDir
			 * @param patchFilePaths
			 * @param failMessage
			 * @param progressMonitor
			 *            프로그래스 다이얼로그에 사용되는 progressMonitor <br>
			 *            null인 경우에도 사용 가능
			 */
			private void copyFiles(final String srcDir, final String destDir, Set<String> patchFilePaths,
					IProgressMonitor progressMonitor, final String failMessage) {
				try {
					PatchFileUtils.copyFilesAndDirs(srcDir, destDir, patchFilePaths, progressMonitor);
				} catch (IOException e1) {
					// 복사 실패
					Display.getDefault().asyncExec(new Runnable() {
						public void run() {
							MessageDialog.openError(null, null, failMessage);
						}
					});
					e1.printStackTrace();
				}
			}
		};
		return selectionAdpater;
	}

	/**
	 * 제품 실행 시 버튼의 포커스를 설정.
	 */
	private void initButtonFocus() {
		if (getMessage().equals(Messages.TitleMessage_0)) {
			patchContainer.setFocusOfButton(buttonNames[INDEX_APPLAY_PATCH_BUTTON]);
		} else if (installPathText.getText().isEmpty()) {
			productBrowseBtn.setFocus();
		} else {
			patchContainer.setFocusOfButton(buttonNames[INDEX_PATCH_HISTORY_BUTTON]);
		}
	}

	/**
	 * 주어진 경로에 주어진 이름의 파일 혹은 디렉터리가 존재하는 지 확인한다. <br>
	 * 
	 * @param searchPath
	 *            최상위 경로
	 * @param searchName
	 *            파일 혹은 디렉터리 이름
	 * 
	 * @return 존재 여부
	 */
	private boolean isValidFileAndDirPath(String searchPath, final String searchName) {
		boolean result = false;

		File path = new File(searchPath);

		if (path.exists()) {
			File searchObj = new File(searchPath + File.separator + searchName);

			if (searchObj.isFile()) {
				File[] identifiers = path.listFiles(new FilenameFilter() {
					public boolean accept(File dir, String name) {
						boolean result = false;
						if (name.equals(searchName)) {
							result = true;
						}
						return result;
					}
				});
				if (identifiers != null && identifiers.length > 0) {
					for (File identifier : identifiers) {
						if (identifier.isFile()) {
							result = true;
							break;
						}
					}
				}

			} else {
				File[] identifiers = path.listFiles(new FilenameFilter() {
					public boolean accept(File dir, String name) {
						boolean result = false;
						if (name.contains(searchName)) {
							result = true;
						}
						return result;
					}
				});
				if (identifiers != null && identifiers.length > 0) {
					for (File identifier : identifiers) {
						if (identifier.isDirectory()) {
							result = true;
							break;
						}
					}
				}
			}
		}
		return result;
	}

	/**
	 * 인자로 받은 디렉터리(절대경로) 혹은 파일이 존재하는 지 여부를 반환
	 * 
	 * @param path
	 * @return
	 */
	private boolean isExist(String path) {
		return new File(path).exists();
	}

	/**
	 * 패치 가능 여부에 따라 버튼의 활성화 여부를 변경한다.
	 * 
	 */
	private void refreshButton() {
		System.out.println("call refreshButton()");

		ErrorCode errorCode = isValidPatch();
		System.out.println("refreshButton errorCode : " + errorCode.name());

		// errorCode에 맞는 메시지로 setMessage 해준다.
		switch (errorCode) {

		case VALID_PATCH:
			setMessage(Messages.TitleMessage_0, IMessageProvider.INFORMATION);
			break;
		case EMPTY_INSTALL_PATH:
			setMessage(Messages.TitleMessage_1, IMessageProvider.INFORMATION);
			break;
		case INVALID_INSTALL_PATH:
			setMessage(Messages.TitleMessage_2, IMessageProvider.WARNING);
			break;
		case EMPTY_PATCH_DIR:
			setMessage(Messages.TitleMessage_3, IMessageProvider.ERROR);
			break;
		case INVALID_PRODUCT:
			setMessage(Messages.TitleMessage_4, IMessageProvider.WARNING);
			break;
		case EQUAL_PATCH_VERSION:
			setMessage(Messages.TitleMessage_5, IMessageProvider.ERROR);
			patchContainer.setEnabledButton(Messages.PatchContentsComposite_ButtonA_0, true);
			patchContainer.setEnabledButton(Messages.PatchContentsComposite_ButtonB_0, false);
			return;
		case LOW_APPLY_PATCH_VERSION:
			setMessage(Messages.TitleMessage_4, IMessageProvider.ERROR);
			patchContainer.setEnabledButton(Messages.PatchContentsComposite_ButtonA_0, isValidRestore());
			patchContainer.setEnabledButton(Messages.PatchContentsComposite_ButtonB_0, false);
			return;
		case RUNNING_PRODUCT:
			setMessage(Messages.TitleMessage_7, IMessageProvider.ERROR);
			break;
		case INVALID_RESTORE:
			setMessage(Messages.TitleMessage_0, IMessageProvider.INFORMATION);
			patchContainer.setEnabledButton(Messages.PatchContentsComposite_ButtonA_0, false);
			patchContainer.setEnabledButton(Messages.PatchContentsComposite_ButtonB_0, true);
			return;
		}

		if (errorCode.equals(ErrorCode.VALID_PATCH)) {
			patchContainer.setEnabledButton(Messages.PatchContentsComposite_ButtonB_0, true);
			patchContainer.setEnabledButton(Messages.PatchContentsComposite_ButtonA_0, true);
			// 처음 패치하는 경우, 패치 이력 버튼을 비활성화 시키기 위한 처리
			if (!isValidRestore()) {
				System.out.println("### FIRST PATCHING ###");
				setMessage(Messages.TitleMessage_0, IMessageProvider.INFORMATION);
				patchContainer.setEnabledButton(Messages.PatchContentsComposite_ButtonA_0, false);
				patchContainer.setEnabledButton(Messages.PatchContentsComposite_ButtonB_0, true);
			}

		} else {
			patchContainer.setEnabledButton(Messages.PatchContentsComposite_ButtonA_0, false);
			patchContainer.setEnabledButton(Messages.PatchContentsComposite_ButtonB_0, false);
		}
	}

	/**
	 * 버튼의 활성화 여부를 판단하기 위해 패치 가능 여부를 판단한다.
	 * 
	 * @return 패치 가능 시 0
	 */
	private ErrorCode isValidPatch() {

		ErrorCode errorCode = ErrorCode.VALID_PATCH;

		// 유틸에 적용할 패치 디렉터리가 존재하는 지 검사
		//
		// 1. 외부 경로에 패치 디렉터리가 존재하는 지 확인,
		// 존재하지 않는다면
		// - 패치 유틸리티 내부 plugins 디렉터리가 존재하는 지 확인
		// 존재한다면
		// - 외부 패치 디렉터리로부터 패치 적용하는 시나리오
		String externalPatchDirPath = PatchFileUtils.getExternalPatchDirPath();

		// 외부 패치 디렉터리가 존재하지 않거나, 패치 파일이 없다면
		if (externalPatchDirPath == null || !PatchExecutor.existPatchfiles(new File(externalPatchDirPath))) {

			// 패치 유틸리티 내부 plugins 디렉터리 검사
			if (!PatchExecutor.existPatchFiles(PatchFileUtils.getJarFile())) {
				errorCode = ErrorCode.EMPTY_PATCH_DIR;
			}
		}
		String installPath = installPathText.getText();
		/** 제품 설치 경로 유효 검사 */
		// 설치 경로가 비어 있는 지 검사
		if (installPath.isEmpty()) { // $NON-NLS-1$
			errorCode = ErrorCode.EMPTY_INSTALL_PATH;
			// 설치 경로에 csc.exe가 존재하는 지 검사
		} else if (!isValidFileAndDirPath(installPath, CSC_EXE)) {
			errorCode = ErrorCode.INVALID_INSTALL_PATH;
			// 적용할 패치가 적용하려는 제품의 패치인 지 검사
		} else if (!isValidApplyPatchForProduct()) {
			errorCode = ErrorCode.INVALID_PRODUCT;
			// 패치를 적용할 제품이 실행 중인 지 검사
		} /*else if (CodeScrollExecutor.isProductStarted(patchInfoData.getProductName())) {			
			패치 구동 시에는 제품 구동 여부를 보지 않고, 패치 적용할 때 보도록 변경. 
			errorCode = ErrorCode.RUNNING_PRODUCT;
			// 설치된 패치보다 적용할 패치의 버전이 높은 지 검사
		} */else {
			switch (isValidApplyPatchVersion()) {
			case -2:
				// 적용할 패치의 버전이 낮음
				errorCode = ErrorCode.LOW_APPLY_PATCH_VERSION;
				break;
			case -1:
				// 적용할 패치와 설치된 패치의 버전이 같음
				errorCode = ErrorCode.EQUAL_PATCH_VERSION;
				break;
			}
		}

		System.out.println("isValidPatch() ERROR CODE: " + errorCode.name());
		return errorCode;
	}

	/**
	 * 적용할 패치가 적용하려는 제품의 패치인 지 확인
	 * <p>
	 * 
	 * 적용할 패치 정보로부터 적용 대상 제품과 패치 버전을 가져와서 설치된 제품의 종류와 버전과 맞는 지 비교한다.
	 */
	private boolean isValidApplyPatchForProduct() {
		boolean result = false;
		String productNameForPatch = patchInfoData.getProductName();
		String productVersionForPatch = patchInfoData.getProductVersion();

		// patch.info에서 적용 제품 버전을 읽어올 때, 파일이 없으면 null을 반환하기 때문에 버전으로 파일의 존재 유무
		// 확인
		if (productVersionForPatch != null) {

			// 패치 적용 대상 제품 정보를 설치된 제품 정보와 비교한다.
			if (localProductName.equals(productNameForPatch) && localProductVersion.equals(productVersionForPatch)) {
				result = true;
			}
		}
		return result;
	}

	/**
	 * 설치된 패치보다 적용할 패치의 버전이 높은 지 확인
	 * 
	 * @return 적용 가능 시 0, <br>
	 *         적용할 패치의 버전이 낮으면 -2,<br>
	 *         적용할 패치와 설치된 패치의 버전이 같으면 -1,<br>
	 */
	private int isValidApplyPatchVersion() {
		int result = 0;
		/**
		 * 설치된 패치보다 설치하려는 패치 버전이 낮거나 같은 지 비교
		 */

		if (!latestPatchedVersion.isEmpty()) {

			String tempLatestVersion = latestPatchedVersion.substring(latestPatchedVersion.lastIndexOf(".") + 1);
			String tempApplyingPatchVersion = patchInfoData.getPatchVersion();
			tempApplyingPatchVersion = tempApplyingPatchVersion
					.substring(tempApplyingPatchVersion.lastIndexOf(".") + 1);

			int latestVersion = Integer.parseInt(tempLatestVersion);
			int applyingPatchVersion = 0;
			try {
				applyingPatchVersion = Integer.parseInt(tempApplyingPatchVersion);
			} catch (NumberFormatException e) {
				// 패치 제공자가 patch.info에 패치 버전을 잘못 명시에 파싱에 실패할 경우 예외처리
				MessageDialog.openError(null, Messages.MessageDialogTitle_7, Messages.Dialog_Message_7);
				System.exit(0);
			}

			if (latestVersion > applyingPatchVersion) {
				// 설치된 패치 버전이 높으면
				result = -2;
			} else if (latestVersion == applyingPatchVersion) {
				// 적용하려는 패치 버전과 같다면
				result = -1;
			}
		} else {
			// 마지막 패치가 없다는 건 처음 패치를 한다고 보고 true를 리턴한다.
			result = 0;
		}

		return result;
	}

	/**
	 * 설치된 제품의 [제품명 제품버전] 을 반환한다.
	 * 
	 */
	private String getProductVersion() {

		// 설치된 제품 경로
		String installPath = installPathText.getText();

		// 제품 설치 경로 검사

		if (isValidFileAndDirPath(installPath, CSC_EXE)) {
			/**
			 * 디렉터리 혹은 파일로부터 제품 패키지명과 제품 버전을 구분하기 위해 패키지명에 '_'를 합친 문자열로 제품 버전을
			 * 알아낸다.
			 */

			final String productPluginsPath = installPath.concat(File.separator + Const.PLUGINS_DIR_NAME);
			if (isExist(productPluginsPath)) {

				if (isValidFileAndDirPath(productPluginsPath, Const.CSUT_PACKAGE_NAME + Const.PACKAGE_NAME_DELIM)) {
					productPackageName = Const.CSUT_PACKAGE_NAME + Const.PACKAGE_NAME_DELIM;
					localProductName = Const.CSUT_NAME;
				} else if (isValidFileAndDirPath(productPluginsPath,
						Const.CSCI_PACKAGE_NAME + Const.PACKAGE_NAME_DELIM)) {
					productPackageName = Const.CSCI_PACKAGE_NAME + Const.PACKAGE_NAME_DELIM;
					localProductName = Const.CSCI_NAME;
				} else if (isValidFileAndDirPath(productPluginsPath,
						Const.CSRTE_PACKAGE_NAME + Const.PACKAGE_NAME_DELIM)) {
					productPackageName = Const.CSRTE_PACKAGE_NAME + Const.PACKAGE_NAME_DELIM;
					localProductName = Const.CSRTE_NAME;
				} else if (isValidFileAndDirPath(productPluginsPath,
						Const.CSDA_PACKAGE_NAME + Const.PACKAGE_NAME_DELIM)) {
					productPackageName = Const.CSDA_PACKAGE_NAME + Const.PACKAGE_NAME_DELIM;
					localProductName = Const.CSDA_NAME;
				} else {
					return "No Detect"; // $NON-NLS-1$
				}

			} else {
				return "No Exist Plugins Directory";
			}

			/**
			 * 제품 설치 경로가 정상이고 설치된 제품 종류를 판단했으므로, 설치된 제품의 버전을 확인한다.
			 * 
			 */
			File root = new File(productPluginsPath);

			if (root.exists()) {
				String[] list = root.list();

				for (String name : list) {
					if (name.indexOf(productPackageName) >= 0) {
						int indexOf = name.indexOf(Const.PACKAGE_NAME_DELIM);
						if (indexOf >= 0) {
							String subString = name.substring(indexOf + 1);
							String[] split = subString.split("\\."); //$NON-NLS-1$

							System.err.println("Product Version: " + split[0] + "." + split[1] + "." + split[2]); //$NON-NLS-1$
							localProductVersion = split[0] + "." + split[1] + "." + split[2]; //$NON-NLS-1$

							return Const.CSENTERPRISE_NAME + " " + localProductName + " " + localProductVersion;
						}
					}
				}
			}
		}
		return "";
	}

	/**
	 * 처음 패치하는 경우인 지 여부 반환
	 * 
	 * @return 되돌릴 파일이 없으면 false 리턴
	 */
	private boolean isValidRestore() {
		List<PatchInfo> patchedInfoList = PatchHistoryParser.getPatchedInfoList(new File(historyPatchFilePath));

		if (patchedInfoList.isEmpty()) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	protected boolean isResizable() {
		return true;
	}

	// 기본 버튼 제거를 위한 오버라이딩
	@Override
	protected Control createButtonBar(Composite parent) {
		return parent;
	}

}