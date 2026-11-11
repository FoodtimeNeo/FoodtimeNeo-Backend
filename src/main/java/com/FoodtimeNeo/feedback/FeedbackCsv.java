package com.FoodtimeNeo.feedback;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** CSV quoting preserves multiline user text; spreadsheet formula prefixes are exported as text. */
public final class FeedbackCsv {
    static final Map<String,String> TYPES=Map.of("function","功能反馈","content","内容反馈","usage","使用反馈","suggestion","改进建议","other","其他");
    static final Map<String,String> STATUSES=Map.of("pending","待处理","processing","处理中","completed","已完成");
    private FeedbackCsv() {}
    public static String cell(String value) {
        String text=value==null?"":value;
        String leading=text.stripLeading();
        if(!leading.isEmpty()&&"=+-@".indexOf(leading.charAt(0))>=0||!text.isEmpty()&&"\t\r\n".indexOf(text.charAt(0))>=0)text="'"+text;
        return text;
    }
    public static String export(List<FeedbackDtos.Row> rows) {
        StringBuilder out=new StringBuilder("\uFEFF反馈编号,反馈类型,反馈内容\r\n");
        for(var row:rows)out.append(quote(row.id().toString())).append(',').append(quote(TYPES.get(row.type())))
                .append(',').append(quote(cell(row.content()))).append("\r\n");
        return out.toString();
    }
    private static String quote(String value) { return "\""+value.replace("\"","\"\"")+"\""; }
    public static List<List<String>> parse(String csv) {
        if(csv.startsWith("\uFEFF"))csv=csv.substring(1);
        List<List<String>> rows=new ArrayList<>();List<String> row=new ArrayList<>();StringBuilder field=new StringBuilder();
        boolean quoted=false,closed=false,started=false;
        for(int i=0;i<csv.length();i++) {
            char c=csv.charAt(i);
            if(quoted) {
                if(c=='"') {
                    if(i+1<csv.length()&&csv.charAt(i+1)=='"'){field.append('"');i++;}
                    else {quoted=false;closed=true;}
                }else field.append(c);
                continue;
            }
            if(c==','||c=='\r'||c=='\n') {
                row.add(field.toString());field.setLength(0);closed=false;started=false;
                if(c!=',') {
                    rows.add(List.copyOf(row));row.clear();
                    if(c=='\r'&&i+1<csv.length()&&csv.charAt(i+1)=='\n')i++;
                }
            }else if(c=='"') {
                if(started||closed)throw new IllegalArgumentException("CSV 引号位置不正确");
                quoted=true;started=true;
            }else {
                if(closed)throw new IllegalArgumentException("CSV 引号结束后只能是逗号或换行");
                field.append(c);started=true;
            }
        }
        if(quoted)throw new IllegalArgumentException("CSV 引号未闭合");
        if(started||closed||!row.isEmpty()){row.add(field.toString());rows.add(List.copyOf(row));}
        return rows;
    }
}
