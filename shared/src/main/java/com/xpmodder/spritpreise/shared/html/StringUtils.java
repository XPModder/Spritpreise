package com.xpmodder.spritpreise.shared.html;

public final class StringUtils {

    public static String ScriptsHtmlToString(String html){
        String out = html.replace("<sup>0", "⁰");
        out = out.replace("<sup>1", "¹");
        out = out.replace("<sup>2", "²");
        out = out.replace("<sup>3", "³");
        out = out.replace("<sup>4", "⁴");
        out = out.replace("<sup>5", "⁵");
        out = out.replace("<sup>6", "⁶");
        out = out.replace("<sup>7", "⁷");
        out = out.replace("<sup>8", "⁸");
        out = out.replace("<sup>9", "⁹");

        out = out.replace("<sub>0", "₀");
        out = out.replace("<sub>1", "₁");
        out = out.replace("<sub>2", "₂");
        out = out.replace("<sub>3", "₃");
        out = out.replace("<sub>4", "₄");
        out = out.replace("<sub>5", "₅");
        out = out.replace("<sub>6", "₆");
        out = out.replace("<sub>7", "₇");
        out = out.replace("<sub>8", "₈");
        out = out.replace("<sub>9", "₉");

        return out;

    }

}
