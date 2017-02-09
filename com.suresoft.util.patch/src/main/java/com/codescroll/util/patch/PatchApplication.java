package com.codescroll.util.patch;

import com.codescroll.util.patch.ui.PatchUtilityDialog;

/**
 * 패치 유틸리티의 메인 클래스
 * @author kbj
 *
 */
public class PatchApplication {

	public static void main(String[] args) {
		PatchUtilityDialog mainDialog = new PatchUtilityDialog(null);
		mainDialog.open();
	}
}