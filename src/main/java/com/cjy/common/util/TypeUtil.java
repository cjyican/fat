package com.cjy.common.util;

public class TypeUtil {
	
	public static Object getInitType(Class<?> clazz) {
		Object result = null;
		try {
			result =  clazz.newInstance();
		} catch (Exception e) {
			if(clazz == Integer.class) {
				result =  0;
			}
			if(clazz == Long.class) {
				result =  0l;
			}
			if(clazz == Double.class) {
				result = 0d;
			}
			if(clazz == Float.class) {
				result = 0f;
			}
			if(clazz == String.class) {
				result =  "";
			}
			if(clazz == Boolean.class) {
				result = false;
			}
			if(clazz == int.class || clazz == long.class || clazz == float.class || clazz == byte.class ) {
				result = 0;
			}
			if(clazz == boolean.class) {
				result = false;
			}
		}
		return result;
	}

}
