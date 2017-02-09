package com.codescroll.util.patch.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.output.FileWriterWithEncoding;

import com.codescroll.util.patch.Const;
import com.codescroll.util.patch.model.PatchInfo;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * 패치 정보를 Json 형식으로 patch.history 파일에 읽고/쓰기 작업을 수행
 * @author Jung
 *
 */
public class PatchHistoryParser {

	private static final String JSON_INDENT = "  ";
	private static final int FIRST_INDEX = 0;

	/**
	 * 패치이력 정보를 파일에 저장한다.
	 * @param patchInfo
	 * @param patchHistory
	 */
	public static void savePatchInfo(PatchInfo patchInfo, File patchHistory){
		List<PatchInfo> patchInfoList = getUpdatedPatchInfoList(patchInfo, patchHistory);
		writeJsonFile(patchInfoList, patchHistory);
	}

	/**
	 * 최신 패치정보가 반영된 패치정보 리스트를 반환한다.
	 * @param patchInfo 최신 패치정보
	 * @param patchHistory 패치이력정보가 저장된 파일
	 * @return List<PatchInfo>
	 */
	private static List<PatchInfo> getUpdatedPatchInfoList(PatchInfo patchInfo, File patchHistory) {
		List<PatchInfo> patchInfoList = getPatchedInfoList(patchHistory);
		patchInfoList.add(FIRST_INDEX, patchInfo);
		return patchInfoList;
	}

	private static void writeJsonFile(List<PatchInfo> patchInfoList, File jsonFile){
	
		Gson gs = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();     
		
	    JsonWriter jsonWriter = null;
	    try {
	        jsonWriter = new JsonWriter(new FileWriterWithEncoding(jsonFile, Const.UTF_8));
	        jsonWriter.setIndent(JSON_INDENT);
	        jsonWriter.beginArray();

	    } catch (UnsupportedEncodingException e) {
	        e.printStackTrace();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }

	    for (PatchInfo feed : patchInfoList) {
	        gs.toJson(feed, PatchInfo.class, jsonWriter);
	    }       

	    try {
	        jsonWriter.endArray();

	    } catch (IOException e) {
	        e.printStackTrace();
	    } finally {
	        try {
	            jsonWriter.close();
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	    }
	}

	/**
	 * patch.history 파일에 있는 가장 최신의 패치 정보를 가져온다. 
	 * @param patchHistory
	 * @return 패치정보가 있을경우 PatchInfo를 반환하고, 없을경우 값이 비어있는 PatchInfo를 반환한다.
	 */
	public static PatchInfo getLatestPatchedInfo(File patchHistory){
		List<PatchInfo> patchInfoList = getPatchedInfoList(patchHistory);

		if(patchInfoList.isEmpty()){
			return new PatchInfo("", "", "", new ArrayList<String>());
		}else{
			return patchInfoList.get(FIRST_INDEX);
		}
	}
	
	/**
	 * patch.histroy 파일에서 가장 최신의 패치 정보를 삭제한다.
	 * @param patchHistory
	 */
	public static void removeLatestPatchedInfo(File patchHistory){
		List<PatchInfo> patchInfoList = getPatchedInfoList(patchHistory);
		if(!patchInfoList.isEmpty()){
			patchInfoList.remove(FIRST_INDEX);
		}
		writeJsonFile(patchInfoList, patchHistory);
	}

	/**
	 * patch.history 파일에 있는 패치정보를 가져온다.
	 * 패치정보는 내림차순으로 리스트에 저장된다. 
	 * @param patchHistory
	 * @return 패치정보를 가져오는데 실패할 경우 비어있는 리스트를 반환하고</br>
	 * 아닐경우, 패치정보를 가진 리스트를 반환한다.
	 */
	public static List<PatchInfo> getPatchedInfoList(File patchHistory){
		List<PatchInfo> patchInfoList = new ArrayList<PatchInfo>();
		Gson gson = new GsonBuilder().create();

		try {

			InputStreamReader inputStreamReader = new InputStreamReader(
					new FileInputStream(patchHistory), Const.UTF_8);

			patchInfoList = gson.fromJson(
					new JsonReader(new BufferedReader(inputStreamReader)),
					new TypeToken<List<PatchInfo>>(){}.getType());

		} catch (JsonIOException e) {
			e.printStackTrace();
			return patchInfoList;
		} catch (JsonSyntaxException e) {
			e.printStackTrace();
			return patchInfoList;
		} catch (FileNotFoundException e) {
			return patchInfoList;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return patchInfoList;
	}
}
