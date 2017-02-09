package com.codescroll.util.patch;

import java.io.File;

/**
 * 공통 상수 클래스
 * @author kbj
 * 
 */
public class Const {
	
	public static final boolean DEBUG = true;
	/**
	 * 이미지 디렉터리 명
	 */
	public static final String IMG_DIR_NAME = "img";
	
	/**
	 * 코너 이미지 아이콘
	 */
	public static final String CS_ICON = IMG_DIR_NAME + "/cs.ico";
	
	/**
	 * 유틸리티 이름
	 */
	public static final String UTILITY_NAME = "CodeScroll Patch Utility";
	
	/**
	 * 유틸을 구성하는 Composite의 기본 Column 수 
	 */
	public static final int DEFAULT_COMPOSITE_COL_NUM = 3;
	
	/**
	 * UTF-8의 charSet
	 */
	public static final String UTF_8 = "UTF-8";
	
	public static final String NEW_LINE = "\n";
	
	public static final String EXCLUDE_PATCH_CONTENTS_SYMBOL="[-]";
	
	/** 
	 * plugins 디렉터리명 
	 */
	public static final String PLUGINS_DIR_NAME = "plugins";
	
	/**
	 * 패치 디렉터리에 포함되는 패치 내역 파일 이름
	 */
	public static final String PATCH_INFO_FILE_NAME = "patch.info";

	/**
	 * 패치 히스토리 파일 이름
	 */
	public static final String PATCH_HISTORY_FILE_NAME = "patch.history";
	
	/**
	 * 백업 파일들을 저장할 최상위 디렉터리명
	 */
	public static final String BACKUP_ROOT_DIR_NAME = "backup";

	/**
	 * INI 파일을 저장할 최상위 디렉터리명
	 */
	public static final String INI_ROOT_DIR_NAME = "patch";
	
	/**
	 * 최초 백업 디렉터리 명
	 */
	public static final String FIRST_PATCH_BACKUP_DIR_NAME = "init";
	
	/** 
	 * INI 파일 디렉터리 경로 
	 */
	public static final String INI_FILE_DIR = System.getenv("APPDATA") 
			+ File.separator + Const.CSENTERPRISE_NAME
			+ File.separator + "utils"
			+ File.separator + Const.INI_ROOT_DIR_NAME;
	
	/** 
	 * INI 파일 경로 
	 */
	public static final String INI_FILE_PATH = Const.INI_FILE_DIR + File.separator + "patch.ini";
	
	/**
	 * 외부 plugins 디렉터리 경로를 얻기 위한 Key
	 */
	public static final String EXTERNAL_PATCH_DIR = "plugins_path";
	
	// 패치 유틸 지원 제품 타입
	
	/**
	 * 패키지 명과 제품 버전을 구분짓는 문자
	 */
	public static final String PACKAGE_NAME_DELIM = "_";
	
	/**
	 * 제품 타입 : 모든 도구 (Enterprise)
	 */
	public static final String CSENTERPRISE_NAME = "CodeScroll";
	
	/**
	 * product type: Controller Tester
	 */
	public static final int CSUT = 100;
	public static final String CSUT_NAME = "Controller Tester";
	public static final String CSUT_PACKAGE_NAME = "com.codescroll.ut";
	
	/**
	 * product type: SNIPER
	 */
	public static final int CSRTE = 200;
	public static final String CSRTE_NAME = "SNIPER";
	public static final String CSRTE_PACKAGE_NAME = "com.codescroll.rte";

	/**
	 * product type: Dependency Analyzer
	 */
	public static final int CSDA = 300;
	public static final String CSDA_NAME = "Dependency Analyzer";
	public static final String CSDA_PACKAGE_NAME = "com.suresofttech.da";
	
	/**
	 * product type: Code Inspector
	 */
	public static final int CSCI = 400;
	public static final String CSCI_NAME = "Code Inspector";
	public static final String CSCI_PACKAGE_NAME = "com.codescroll.ci";
	
	

}
