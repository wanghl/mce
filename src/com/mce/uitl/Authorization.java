package com.mce.uitl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSON;

public class Authorization {

	private static String getAuthorizationJsonString() throws IOException {
		InputStream in = new FileInputStream ( Authorization.class.getResource("/authorization.json").getFile() ) ;
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		String jsonstring = reader.readLine();
		reader.close();
		return jsonstring;
	}

	public static boolean authorizationChecking(Long currentPositionAmount) {
		try {
			String v = getAuthorizationJsonString();
			if (v == null)
				return false;

			HashMap jsObjMap = JSON.parseObject(v, HashMap.class);

			if (jsObjMap.get("k") == null)
				return false;

			DES des = new DES();
			des.getKey("6monthwin");
			Integer L = Integer.parseInt(des.decode(jsObjMap.get("k").toString()));

			return (currentPositionAmount >= L) ? false : true;
		} catch (Exception e) {
			return false;
		}
	}

}
