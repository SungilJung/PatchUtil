package com.codescroll.util.patch.model;


/**
 * 제품 정보를 갖고 있는 model
 * @author Jung
 *
 */
/**
 * @author Jung
 *
 */
/**
 * @author Jung
 *
 */
public class CSProduct {
	/** 제품 버전(minor version 까지) */
	String version;
	/** 제품 명 */
	String name;
	/** 제품 설치 경로 */
	String path;
	
	public CSProduct(String version, String name, String path) {
		this.version = (version == null) ? "" : version;
		this.name = (name == null) ? "" : name;
		this.path = (path == null) ? "" : path;
	}
	
	/**
	 * 제품 버전을 반환한다.
	 * @return 버전이 없는 경우 ""를 반환한다.
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * 제품 명을 반환한다.
	 * @return 제품 명이 없는 경우 ""을 반환한다.
	 */
	public String getName() {
		return name;
	}

	/**
	 * 제품 경로를 반환한다.
	 * @return 제품 경로가 없는 경우 ""을 반환한다.
	 */
	public String getPath() {
		return path;
	}
	
	@Override
	public int hashCode() {
		return name.hashCode() + version.hashCode();
	}

	@Override
	public boolean equals(Object obj) {

		if (obj == null) {
			return false;
		}

		if (this.getClass() != obj.getClass()) {
			return false;
		}

		if (this == obj) {
			return true;
		}

		CSProduct that = (CSProduct) obj;

		if (this.name.equals(that.name) && this.version.equals(that.version)) {
			return true;
		}

		return false;
	}
}
