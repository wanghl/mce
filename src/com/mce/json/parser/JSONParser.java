package com.mce.json.parser;


import com.alibaba.fastjson.JSONObject;

public class JSONParser {
	
	
	
	
	public Object getJsonValue(String model,JSONObject jsonobject)
	{
		Object o  = null;
		try{
		String[] array = model.split("\\.") ;
		int index = array.length -1 ;
		for(String key : array)
		{
			if(index == 0)
			{
				o = jsonobject.get(key);
			}else
			{
				jsonobject = jsonobject.getJSONObject(key);
			}
			-- index ;
		}
		}
		catch (Exception e)
		{
			//throw new RuntimeException(e) ;
			return null ;
		}
		return o ;
	}

}
