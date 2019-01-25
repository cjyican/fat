package com.cjy.fat.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.cjy.fat.exception.FatTransactionException;

public class CollectionUtil {

	private CollectionUtil() {

	}

	public static void checkDataSet(Set<String> dataSet, String txKey) {
		if (null == dataSet || dataSet.size() == 0) {
			throw new FatTransactionException(txKey, "empty service list");
		}
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
	
	public static void main(String[] args) {
		System.out.println(covertStringToCollection(" "));
	}

}
