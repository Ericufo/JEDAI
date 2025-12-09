package com.github.ericufo.jedai.rag.strategy.impl;

import com.github.ericufo.jedai.rag.CourseMaterial;
import com.github.ericufo.jedai.rag.strategy.ParserStrategy;
import com.github.ericufo.jedai.rag.strategy.ParsingException;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.TextDocumentParser;

import java.io.IOException;
import java.nio.file.Path;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;

/**
 * 文本解析策略实现
 * 使用LangChain4j的TextDocumentParser解析文本文档
 */
public class TextParserStrategy implements ParserStrategy {
    
    @Override
    public Document parse(CourseMaterial material) throws ParsingException, IOException {
        if (!canParse(material)) {
            throw new ParsingException("Cannot parse material: " + material.getFile().getName());
        }
        
        Path path = material.getFile().toPath();
        return loadDocument(path, new TextDocumentParser());
    }
    
    @Override
    public boolean canParse(CourseMaterial material) {
        return material.getType() == CourseMaterial.MaterialType.TEXT;
    }
    
    @Override
    public String getStrategyName() {
        return "TextParser";
    }
}