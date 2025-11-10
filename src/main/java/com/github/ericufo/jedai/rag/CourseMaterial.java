package com.github.ericufo.jedai.rag;

import java.io.File;

/**
 * 课程材料表示
 */
public class CourseMaterial {
    private final File file;
    private final MaterialType type;

    public CourseMaterial(File file, MaterialType type) {
        this.file = file;
        this.type = type;
    }

    public File getFile() {
        return file;
    }

    public MaterialType getType() {
        return type;
    }

    public enum MaterialType {
        PDF,
        TEXT
    }
}

