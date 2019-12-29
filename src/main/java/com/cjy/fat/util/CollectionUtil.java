package com.cjy.fat.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

public class CollectionUtil {

	private CollectionUtil() {

	}

	public static Set<String> covertStringToCollection(String string) {
		if (StringUtils.isNotBlank(StringUtils.trim(string))) {
			Set<String> set = new HashSet<>();
			String[] strArray = StringUtils.split(string, ",");
			set.addAll(Arrays.asList(strArray));
			return set;
		}
		return null;
	}

}
