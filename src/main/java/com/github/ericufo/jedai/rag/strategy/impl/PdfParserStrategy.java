package com.github.ericufo.jedai.rag.strategy.impl;

import com.github.ericufo.jedai.rag.CourseMaterial;
import com.github.ericufo.jedai.rag.strategy.ParserStrategy;
import com.github.ericufo.jedai.rag.strategy.ParsingException;
import dev.langchain4j.data.document.Document;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;

/**
 * PDF解析策略实现
 * 使用PDFBox解析PDF文档
 */
public class PdfParserStrategy implements ParserStrategy {
    
    @Override
    public Document parse(CourseMaterial material) throws ParsingException, IOException {
        if (!canParse(material)) {
            throw new ParsingException("Cannot parse material: " + material.getFile().getName());
        }
        
        try (PDDocument pdfDocument = PDDocument.load(material.getFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(pdfDocument);
            return new Document(text);
        }
    }
    
    @Override
    public boolean canParse(CourseMaterial material) {
        return material.getType() == CourseMaterial.MaterialType.PDF;
    }
    
    @Override
    public String getStrategyName() {
        return "PdfParser";
    }
}