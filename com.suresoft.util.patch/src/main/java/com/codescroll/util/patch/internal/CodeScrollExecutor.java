package com.codescroll.util.patch.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import com.codescroll.util.patch.Const;

/**
 * 현재 패치 대상의 제품 실행기
 * @author Jung
 *
 */
public class CodeScrollExecutor {

	public static final String EXE_FILE_NAME = "CodeScroll.exe";
	private static final String CLEAN_OPTION = "-clean";
	
	/**
	 * 새로운 스레드에서 인자로 받은 제품 경로의 CodeScroll.exe를 clean 옵션으로 실행한다.
	 * 
	 * @param installPath 해당 재품의 설치 경로
	 * 
	 */
	public static void executeCodeScroll(String installPath){

		String exePath = installPath + File.separator + EXE_FILE_NAME;
		if(!canExecute(exePath)){

			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					MessageDialog.openError(null, Messages.MessageDialogTitle_6, Messages.Dialog_Message_6); 
				}
			});
			return;
		}

		final DefaultExecutor executor = new DefaultExecutor();
		final CommandLine cmdLine = new CommandLine(exePath);
		cmdLine.addArgument(CLEAN_OPTION);

		Executors.defaultThreadFactory().newThread(new Runnable() {
			public void run() {
				try {
					executor.execute(cmdLine);
				} catch (ExecuteException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	/**
	 * 인자로 받은 exe 경로가 실행 가능한 지 여부를 반환한다.
	 * 
	 * @param exePath
	 * @return
	 */
	private static boolean canExecute(String exePath){

		File exeFile = new File(exePath);
		return exeFile.canExecute();
	}

	/**
	 * 실행 중인 제품을 확인한다. <br>
	 * 실행 제품명과 PID를 담은 Map을 반환한다. <p>
	 * 
	 * CodeScroll 제품이 실행 중인 지 확인하는 방법 중 설치 경로 명을 rename하는 방식을 사용하려 했으나, <br>
	 * 1. A file handle is inherited by a subprocess of your process <br>
	 * 2. An anti-virus program is scanning the file for viruses, and so has it open <br>
	 * 3. An indexer (such as Google Desktop or the Windows indexing service) has the file open <br>
	 * 등의 이유로 비정상적으로 rename이 실패하는 경우가 있어서 사용하지 않음. <p>
	 * 
	 */
	public static Map<String, String> getStartedProductsInfo() {
		Map<String, String> productPidMap = new HashMap<String, String>();
		try {
			String line;
			Process p = Runtime.getRuntime().exec("tasklist.exe /fo csv /nh /fi \"IMAGENAME eq CodeScroll.exe\" /v"); //$NON-NLS-1$
			BufferedReader input = new BufferedReader
					(new InputStreamReader(p.getInputStream(), "EUC-KR"));
			while ((line = input.readLine()) != null) {
				if (!line.trim().equals("")) {
					// keep only the process name
					String[] split = line.split(",");
					
					if(split.length > 1) {
						String pid = split[1];
						String runProgram = split[split.length - 1];
						
						if(runProgram.contains("CodeScroll ")) {
							String runProduct = runProgram.substring(1, runProgram.lastIndexOf("\""));
							runProduct = runProduct.substring(runProduct.indexOf(' ') + 1);

							if(runProduct.equals(Const.CSCI_NAME)) {
								productPidMap.put(Const.CSCI_NAME, pid);
							} else if(runProduct.equals(Const.CSUT_NAME)) {
								productPidMap.put(Const.CSUT_NAME, pid);
							}else if(runProduct.equals(Const.CSRTE_NAME)) {
								productPidMap.put(Const.CSRTE_NAME, pid);
							}
						}
					}
				}
			}

			p = Runtime.getRuntime().exec("tasklist.exe /fo csv /nh /fi \"IMAGENAME eq DA.exe\" /v"); //$NON-NLS-1$
			input = new BufferedReader
					(new InputStreamReader(p.getInputStream()));
			while ((line = input.readLine()) != null) {

				if (!line.trim().equals("")) {
					// keep only the process name
					String[] split = line.split(",");
					
					if(split.length > 1) {
						String pid = split[1];
						String runProgram = split[split.length - 1];

						if(runProgram.contains("Dependency ")) {
							String runProduct = runProgram.substring(1, runProgram.lastIndexOf("\""));

							if(runProduct.equals(Const.CSDA_NAME)) {
								productPidMap.put(Const.CSDA_NAME, pid);
							}
						}
					}
				}
			}
			input.close();

			if(!productPidMap.isEmpty()) {
				return productPidMap;
			}
		}
		catch (Exception err) {
			err.printStackTrace();
		}
		return productPidMap;
	}
	
	/**
	 * 주어진 도구명을 사용해 도구가 실행되고 있는 지 여부를 반환.
	 * 
	 * @param productName Const.CSCI_NAME, Const.CSCT_NAME, Const.CSRTE_NAME, Const.CSDA_NAME
	 * @return
	 */
	public static boolean isProductStarted(String productName) {
		
		Map<String, String> productPidMap = getStartedProductsInfo();
		
		return productPidMap.containsKey(productName);
	}
	
	/**
	 * 인자로 받은 프로세스 pid로 프로세스를 종료시킨다.
	 * 
	 * @param pid
	 */
	public static void processKill(String pid) {
		try {
			Runtime.getRuntime().exec("taskkill.exe /f /pid " + pid); //$NON-NLS-1$
		}
		catch (Exception err) {
			err.printStackTrace();
		}
	}
}
