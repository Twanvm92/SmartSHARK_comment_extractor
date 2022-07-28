package org.example;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.CommentsCollection;

import java.util.List;
import java.util.Optional;

public class Main {
    void Hello() {

        System.out.println("Hello");
    }
    public static void main(String[] args) {
        System.out.println("Hello world!");
//        StaticJavaParser.getConfiguration().setPreprocessUnicodeEscapes(true);
//        CompilationUnit cu = StaticJavaParser.parse("\u002f\u002a <- multi line comment start */\n");
        JavaParser parser = new JavaParser();
        ParseResult<CompilationUnit> pr = parser.parse("+ for (final RDFSyntax s : RDFSyntax.w3cSyntaxes()) { \n" +
                "+ // this is a line comment");



        Optional<CommentsCollection> comments  = pr.getCommentsCollection();
        System.out.println(comments.orElseThrow().getComments().first().getContent());
//        if (comments.size() > 0) {
//            System.out.println(comments.get(0).getContent());
//        }
    }

}