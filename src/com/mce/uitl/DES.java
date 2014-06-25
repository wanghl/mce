package com.mce.uitl;

/*   
 *   褰撳墠鏂囦欢锛欴ES.java   
 *   鍒涘缓鏃ユ湡锛�013-01-22   
 *   鐗�鏈�鍙凤細1.0
 *   浣滆�锛歵ianfm@yonyou.com
 *     
 */

import java.security.Key;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;



public class DES {
	Key key;


	public void getKey(String secretKey) {
		try {
			KeyGenerator _generator = KeyGenerator.getInstance("DES");
			SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
			secureRandom.setSeed(secretKey.getBytes());
			_generator.init(secureRandom);
			this.key = _generator.generateKey();
			_generator = null;
		} catch (Exception e) {
			//e.printStackTrace();
		}
	}

	/**
	 * 鍔犲瘑String鏄庢枃杈撳叆,String瀵嗘枃杈撳嚭
	 * 
	 * @param strMing
	 * @return
	 */
	public String encode(String mingTxt) {
		byte[] byteMi = null;
		byte[] byteMing = null;
		String miStr = "";
		BASE64Encoder base64en = new BASE64Encoder();
		try {
			byteMing = mingTxt.getBytes("UTF8");
			byteMi = this.encode(byteMing);
			miStr = base64en.encode(byteMi);
		} catch (Exception e) {
			//e.printStackTrace();
		} finally {
			base64en = null;
			byteMing = null;
			byteMi = null;
		}
		miStr  = miStr.replaceAll("\\r\\n", "");
		return miStr;
	}

	/**
	 * 瑙ｅ瘑 浠tring瀵嗘枃杈撳叆,String鏄庢枃杈撳嚭
	 * 
	 * @param miTxt
	 * @return
	 */
	public String decode(String miTxt) {
		BASE64Decoder base64De = new BASE64Decoder();
		byte[] byteMing = null;
		byte[] byteMi = null;
		String mingStr = "";
		try {
			byteMi = base64De.decodeBuffer(miTxt);
			byteMing = this.decode(byteMi);
			mingStr = new String(byteMing, "UTF8");
		} catch (Exception e) {
			//e.printStackTrace();
		} finally {
			base64De = null;
			byteMing = null;
			byteMi = null;
		}
		return mingStr;
	}

	/**
	 * 鍔犲瘑浠yte[]鏄庢枃杈撳叆,byte[]瀵嗘枃杈撳嚭
	 * 
	 * @param byteS
	 * @return
	 */
	private byte[] encode(byte[] byteS) {
		byte[] byteFina = null;
		Cipher cipher;
		try {
			cipher = Cipher.getInstance("DES");
			cipher.init(Cipher.ENCRYPT_MODE, key);
			byteFina = cipher.doFinal(byteS);
		} catch (Exception e) {
			//e.printStackTrace();
		} finally {
			cipher = null;
		}
		return byteFina;
	}

	/**
	 * 瑙ｅ瘑浠yte[]瀵嗘枃杈撳叆,浠yte[]鏄庢枃杈撳嚭
	 * 
	 * @param byteD
	 * @return
	 */
	private byte[] decode(byte[] byteD) {
		Cipher cipher;
		byte[] byteFina = null;
		try {
			cipher = Cipher.getInstance("DES");
			cipher.init(Cipher.DECRYPT_MODE, key);
			byteFina = cipher.doFinal(byteD);
		} catch (Exception e) {
			//e.printStackTrace();
		} finally {
			cipher = null;
		}
		return byteFina;

	}

	public String getString(String str){
		String key = "ecology";
	    this.getKey(key);// 鐢熸垚瀵嗗寵
		return this.decode(str);
	}
	/**
	 * 绗竴涓弬鏁帮細瀵嗗寵 绗簩涓弬鏁帮細绫诲瀷锛圖 -> 瑙ｅ瘑, E -> 鍔犲瘑锛�绗笁涓弬鏁帮細瀛楃涓诧紝濡傛灉涓篋锛屾槸瀵嗘枃锛屽鏋滀负E锛屽垯鏄庢枃
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		DES des = new DES();// 瀹炰緥鍖栦竴涓鍍�
		String key = "msfl";
	    des.getKey(key);// 鐢熸垚瀵嗗寵

	    System.out.println("瀵嗛挜="+key);
	    System.out.println("鍘熸枃="+des.encode("20131031010"));
	    System.out.println("杩樺師鏄庢枃="+des.decode("jVgMa8Ku/HXdLtswLlQh2w=="));
	}

}

