package com.codescroll.util.patch.ui.dialog;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.codescroll.util.patch.Const;
import com.codescroll.util.patch.internal.CodeScrollExecutor;
import com.codescroll.util.patch.internal.Messages;
import com.codescroll.util.patch.internal.PatchExecutor;
import com.codescroll.util.patch.internal.PatchFileUtils;
import com.codescroll.util.patch.internal.PatchHistoryParser;
import com.codescroll.util.patch.model.PatchInfo;
import com.codescroll.util.patch.ui.PatchUtilityDialog;
import com.codescroll.util.patch.ui.composite.PatchContentsContainer;

/**
 * 패치 이력을 보여주는 dialog
 * @author Jung
 *
 */
public class PatchHistoryDialog extends Dialog {

	private static final int INDEX_RESTORE_BUTTON = 0;
	private static final int INDEX_CANCEL_BUTTON = 1;

	/**	외부 경로로부터 패치를 적용할 지 여부 */
	private final boolean EXTERNAL_MODE;
	{
		System.out.println("##### HistoryDialog EXTERNAL MODE initialize #####");
		
		String externalPatchDirPath = PatchFileUtils.getExternalPatchDirPath();

		// 외부 패치 디렉터리가 존재하지 않거나, 패치 파일이 없다면
		if(externalPatchDirPath != null && 
				PatchExecutor.existPatchfiles(new File(externalPatchDirPath))) {
			EXTERNAL_MODE = true;
		} else {
			EXTERNAL_MODE = false;
		}
	}
	private PatchContentsContainer patchContentsComposite;

	private PatchInfo latestPatchedInfo;

	private String[] buttonNames = new String[]{ 
			Messages.PatchContentsComposite_ButtonA_1, 
			Messages.PatchContentsComposite_ButtonB_1
	};

	/** 최상위 백업 디렉터리 경로*/
	private String backupRootPath = "";
	/** 설치된 제품의 plugins 디렉터리 경로*/
	private String pluginsDirPath = "";
	/** patch.history 파일 경로*/
	private String patchHistoryFilePath = "";
	/** 백업 파일이 저장되어 있는 디렉터리 경로*/
	private String backupDirPath = "";

	public PatchHistoryDialog(Shell parent, String installPath){
		super(parent);

		this.pluginsDirPath = installPath + File.separator + Const.PLUGINS_DIR_NAME;
		this.backupRootPath = installPath + File.separator + Const.BACKUP_ROOT_DIR_NAME;
		this.patchHistoryFilePath = backupRootPath + File.separator + Const.PATCH_HISTORY_FILE_NAME;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(Messages.PatchContentsComposite_ButtonA_0);
		InputStream in = ImageLoader.class.getClassLoader().getResourceAsStream(Const.CS_ICON);
		// 이미지가 없더라도 오류 없이 실행되도록 예외처리
		if(in != null) {
			Image img = new Image(null, in);
			newShell.setImage(img);
		}
	}

	@Override
	protected Control createDialogArea(Composite parent) {

		GridLayout gdLayout = new GridLayout();
		gdLayout.numColumns = Const.DEFAULT_COMPOSITE_COL_NUM;

		Composite parentArea = (Composite) super.createDialogArea(parent);
		Composite container = new Composite(parentArea, SWT.NONE);

		container.setLayout(gdLayout);
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		patchContentsComposite = new PatchContentsContainer(container, buttonNames);
		patchContentsComposite.createPartControl(650, PatchContentsContainer.HEIGHT_HINT);

		return parent;
	}

	@Override
	protected Control createContents(Composite parent) {
		Control control = super.createContents(parent);

		latestPatchedInfo = PatchHistoryParser.getLatestPatchedInfo(new File(patchHistoryFilePath));
		backupDirPath = createBackupDirPath();
		patchContentsComposite.setViewerContents(createViewerContents());
		registerSelectionListener();
		initButtonFocus();

		return control;
	}

	private String createBackupDirPath() {

		String backupDirPath = backupRootPath;
		String patchVersion = creatBackupVersion();

		return backupDirPath += File.separator + patchVersion;
	}

	private String creatBackupVersion() {
		List<PatchInfo> patchedInfoList = PatchHistoryParser.getPatchedInfoList(new File(patchHistoryFilePath));

		if(patchedInfoList.isEmpty() || patchedInfoList.size() == 1){
			return Const.FIRST_PATCH_BACKUP_DIR_NAME;
		}
		return patchedInfoList.get(1).getPatchVersion();
	}

	/**
	 * @return 패치 내용 뷰어에 들어가는 컨텐츠를 반환한다.
	 */
	private String createViewerContents() {

		File historyFile = new File(patchHistoryFilePath);
		StringBuilder contents = new StringBuilder();

		if(historyFile.exists()) {
			List<PatchInfo> patchHistoryList = PatchHistoryParser.getPatchedInfoList(historyFile);

			for(PatchInfo data : patchHistoryList) {
				contents.append(data.toString());
			}
		}

		String result = contents.toString();
		if(result.isEmpty()){
			result = Messages.PatchContentsMessage;
		}

		return result;
	}

	/**
	 * 다이얼로그 실행 시 버튼의 포커스를 설정.
	 */
	private void initButtonFocus() {
		patchContentsComposite.setFocusOfButton(buttonNames[INDEX_RESTORE_BUTTON]);
	}

	/**
	 * 각 버튼에 SelectionListener를 등록한다.
	 */
	private void registerSelectionListener() {
		patchContentsComposite.registerButtonSelectionAdpater(
				buttonNames[INDEX_RESTORE_BUTTON], createRestoreSelectionAdapter());

		patchContentsComposite.registerButtonSelectionAdpater(
				buttonNames[INDEX_CANCEL_BUTTON], createCancelSelectionAdapter());
	}


	private SelectionAdapter createRestoreSelectionAdapter(){

		SelectionAdapter restoreAction = new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				try {
					Map<String, String> startedProductNames = CodeScrollExecutor.getStartedProductsInfo();
					if(!startedProductNames.isEmpty()) {
//						여기서 어떻게 현재 패치하려는 제품과 실행중인 제품을 비교하여 예외처리를 할 것이냐?
						openRestoreFailMessageDialog();
						setReturnCode(CANCEL);

					}else{
						boolean restoreAnswer = openConfirmRestoreDialog();

						if(restoreAnswer) {

							final ProgressMonitorDialog pmd = new ProgressMonitorDialog(Display.getDefault().getActiveShell());

							pmd.run(true, false, new IRunnableWithProgress() {

								public void run(
										IProgressMonitor paramIProgressMonitor)
												throws InvocationTargetException,
												InterruptedException {

									int totalCount;
									if(EXTERNAL_MODE) {
										totalCount = PatchExecutor.getPatchFilePathList(PatchFileUtils.getExternalPatchDirPath()).size() * 2;
									} else {
										totalCount = PatchExecutor.getPatchFilePathList(PatchFileUtils.getJarFile()).size() * 2;
									}
									
									paramIProgressMonitor.beginTask(Messages.ProgressMessage_1, totalCount);

									if(!existBackupFiles(latestPatchedInfo.getPatchFilePathList())){
										// 백업 파일이 없다는 것을 나타내는 다이얼로그
										Display.getDefault().asyncExec(new Runnable() {
											public void run() {
												MessageDialog.openError(null, Messages.MessageDialogTitle_3, Messages.Dialog_Message_3);
											}
										});
										setReturnCode(CANCEL);

									}else{
										// 되돌리기 수행
										Set<String> patchFilePathList = getPatchFilePathList(latestPatchedInfo.getPatchFilePathList());
										try {
											removePatchedFiles(patchFilePathList);
											PatchFileUtils.copyFilesAndDirs(backupDirPath, pluginsDirPath,
													patchFilePathList, paramIProgressMonitor);
											removeBackupData();
											setReturnCode(OK);
										} catch (IOException e) {
											// 되돌리기 실패 
											Display.getDefault().asyncExec(new Runnable() {
												public void run() {
													MessageDialog.openError(null, null, Messages.Dialog_Message_8);
												}
											});
											e.printStackTrace();
											setReturnCode(CANCEL);
										}
									}
								}
							});
							close();
						} 
					}
				} catch (InvocationTargetException e1) {
					e1.printStackTrace();
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}

			private void openRestoreFailMessageDialog() {
				MessageDialog.openWarning(
						getShell(),
						Messages.MessageDialogTitle_3, Messages.Dialog_Message_1);
			}

			private boolean openConfirmRestoreDialog() {

				boolean restoreAnswer = MessageDialog.openQuestion(
						getShell(), Messages.MessageDialogTitle_4, getRestoreMessage());

				return restoreAnswer;
			}

			private String getRestoreMessage(){
				String message ="";
				List<PatchInfo> patchedInfoList = PatchHistoryParser.getPatchedInfoList(new File(patchHistoryFilePath));
				if(patchedInfoList.size() == 1){
					message = Messages.Dialog_Message_4;
				}else{
					String version = "[" + patchedInfoList.get(1).getPatchVersion() + "]";
					message = Messages.bind(Messages.Dialog_Message_5, version);
				}
				return message;
			}

			private boolean existBackupFiles(Set<String> patchFilePathList) {
				boolean existBackupFile = true;
				for(String patchFilePath : patchFilePathList){
					if(!patchFilePath.contains("*")){

						File backupFile = new File(backupDirPath + File.separator + patchFilePath);

						if(!backupFile.exists()){
							existBackupFile = false;
							break;
						}
					}
				}
				return existBackupFile;
			}

			private Set<String> getPatchFilePathList(Set<String> patchFilePathList) {
				Set<String> resultPathList = new HashSet<String>();
				for(String patchFilePath : patchFilePathList){
					if(patchFilePath.contains("*")){
						patchFilePath = patchFilePath.replace("*","");
					}
					resultPathList.add(patchFilePath);
				}
				return resultPathList;
			}
			
			private void removePatchedFiles(Set<String> patchFilePathList) throws IOException {
				for(String filePath : patchFilePathList){
					String patchFilePath = pluginsDirPath + File.separator + filePath;
					FileUtils.forceDelete(new File(patchFilePath));
				}
			}

			/**
			 * 패치 되돌리기 수행을 성공하면, <br>
			 * patch.history 파일을 마지막 패치 내역 및 되돌린 백업 파일 디렉터리를 삭제한다.
			 */
			private void removeBackupData() {
				PatchHistoryParser.removeLatestPatchedInfo(new File(patchHistoryFilePath));
				FileUtils.deleteQuietly(new File(backupDirPath));
			}

		};

		return restoreAction;
	}

	private SelectionAdapter createCancelSelectionAdapter() {
		SelectionAdapter closeDialogAction = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setReturnCode(CANCEL);
				close();
			}
		};
		return closeDialogAction;
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
