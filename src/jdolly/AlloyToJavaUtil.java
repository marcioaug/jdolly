package jdolly;

import jdolly.util.StrUtil;

import java.util.ArrayList;
import java.util.List;

public final class AlloyToJavaUtil {

    private AlloyToJavaUtil(){}

    public static String cleanName(final String name) {
        final int beginIndex = 1;
        final int endIndex = name.length() - 1;

        String removeBraces = name.substring(beginIndex, endIndex);
        String replaceDollar = removeBraces.replace("$", StrUtil.UNDERSCORE_SYMBOL);
        String removeSpaces = replaceDollar.replaceAll(" ", "");

        return removeSpaces;
    }

    public static String removeCrap(String instance) {
        String aux = instance.replaceAll("[^/]*/", "");
        return aux;
    }

    public static List<String> getNames(final String[] types){
        List<String> result = new ArrayList<String>();

        final char empty = ' ';

        final int beginIndex = 1;
        final int firstPosition = 0;

        for (String typeName : types) {
            if (typeName.charAt(firstPosition) == empty)
                typeName = typeName.substring(beginIndex);
            typeName = typeName.replaceAll("javametamodel(.)*/", "");
            result.add(typeName);
        }
        return result;
    }

    public static List<String> extractInstances(String labels) {
        List<String> result = new ArrayList<String>();

        String instances = cleanName(labels);

        boolean instancesIsNotEmpty = instances.length() > 0;

        if (instancesIsNotEmpty) {
            String[] types = instances.split(",");
            result = getNames(types);
        }
        return result;
    }
}