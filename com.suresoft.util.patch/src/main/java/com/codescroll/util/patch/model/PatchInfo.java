package com.codescroll.util.patch.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.codescroll.util.patch.Const;
import com.codescroll.util.patch.internal.Messages;

/**
 * 패치 정보 모델<br>
 * 패치 내역과 패치 히스토리를 다룰 때 사용
 * 
 * @author Jung
 *
 */
public class PatchInfo{
	
	private static final String PREFIX_STRAPLINE = "* ";

	/** 제품 명 */
	private String productName;
	
	/** 제품 버전 */
	private String productVersion;
	
	/** 패치 버전 */
	private String patchVersion;
//	/** 패치 내용 */
//	private String patchContents;
	private List<String> contentsList = new ArrayList<String>();
	/** 적용된 패치파일 경로 리스트 */
	private Set<String> patchFilePathList = new HashSet<String>();
	
	/**
	 * 생성자
	 * @param productName
	 * @param productVersion
	 * @param patchVersion
	 * @param contentsList
	 */
	public PatchInfo(String productName, String productVersion, 
			String patchVersion, List<String> contentsList) {
		this.productName = productName;
		this.productVersion = productVersion;
		this.patchVersion = patchVersion;
		this.contentsList.addAll(contentsList);
	}
	
	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public String getProductVersion() {
		return productVersion;
	}

	public void setProductVersion(String productVersion) {
		this.productVersion = productVersion;
	}

	public String getPatchVersion() {
		return patchVersion;
	}
	
	public void setPatchVersion(String patchVersion) {
		this.patchVersion = patchVersion;
	}

	public List<String> getContentsList() {
		return contentsList;
	}

	public void setContentsList(List<String> contentsList) {
		this.contentsList = contentsList;
	}

	public Set<String> getPatchFilePathList() {
		return patchFilePathList;
	}

	public void setPatchFilePathList(Collection<String> patchFileList) {
		this.patchFilePathList.clear();
		this.patchFilePathList.addAll(patchFileList);
	}

	@Override
	public String toString() {
		String patchVersionMessage = PREFIX_STRAPLINE + Messages.PatchVersion + patchVersion +Const.NEW_LINE;
		String patchContentsMessage = getPatchMessage();
		return patchVersionMessage + patchContentsMessage + Const.NEW_LINE;	
	}

	private String getPatchMessage() {
		String patchContentsMessage = PREFIX_STRAPLINE + Messages.PatchContents +Const.NEW_LINE;
		String contentsMessage = "";
		
		for(String contents : contentsList){
			if(!contents.contains(Const.EXCLUDE_PATCH_CONTENTS_SYMBOL)){
				contentsMessage += contents + Const.NEW_LINE;
			}
		}
		
		if(contentsMessage.isEmpty()){
			contentsMessage += "- " + Messages.Empty_patch_message + Const.NEW_LINE;
		}
		
		return patchContentsMessage + contentsMessage;
	}
}
