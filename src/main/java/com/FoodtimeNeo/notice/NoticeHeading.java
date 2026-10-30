package com.FoodtimeNeo.notice;
import org.commonmark.node.*;
import org.commonmark.parser.Parser;
public final class NoticeHeading {
    public static String title(String content) {
        class Headings extends AbstractVisitor {
            int level=7; String title="";
            @Override public void visit(Heading h) {
                if(h.getLevel()>=level) return;
                StringBuilder text=new StringBuilder();
                h.accept(new AbstractVisitor() {
                    @Override public void visit(Heading n) { visitChildren(n); }
                    @Override public void visit(Text n) { text.append(n.getLiteral()); }
                    @Override public void visit(Code n) { text.append(n.getLiteral()); }
                    @Override public void visit(SoftLineBreak n) { text.append(' '); }
                    @Override public void visit(HardLineBreak n) { text.append(' '); }
                });
                level=h.getLevel();title=text.toString().strip();
            }
        }
        Headings headings=new Headings();Parser.builder().build().parse(content).accept(headings);return headings.title;
    }
    private NoticeHeading() {}
}
