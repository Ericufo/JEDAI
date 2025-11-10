package com.github.ericufo.jedai.rag.impl;

import com.github.ericufo.jedai.rag.CourseMaterial;
import com.github.ericufo.jedai.rag.RagIndexer;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import com.google.gson.Gson;

/**
 * Automatically build RAG index when the project is opened
 */
public class RagIndexerStartupActivity implements StartupActivity {
    private static final Logger LOG = Logger.getInstance(RagIndexerStartupActivity.class);
    private static final String MATERIALS_CACHE_FILE = "rag_materials_cache.json"; // Material cache file, fixed in root directory

    /**
     * Main activity method that runs when the project is opened
     * 
     * @param project the current project instance
     */
    @Override
    public void runActivity(@NotNull Project project) {
        try {
            Path basePath = Paths.get(Objects.requireNonNull(project.getBasePath()));
            Path cachePath = basePath.resolve(MATERIALS_CACHE_FILE);

            RagIndexer indexer = new SimpleRagIndexer();

            // Collect materials
            List<CourseMaterial> materials = collectCourseMaterials(project);

            // Check if materials have changed
            if (materialsChanged(materials, cachePath)) {
                LOG.info("Detected changes in course materials, clearing and rebuilding index");
                indexer.clearIndex();
                updateMaterialsCache(materials, cachePath);
            } else if (indexer.isIndexed()) {
                LOG.info("RAG index already exists and materials unchanged, skipping build");
                return;
            }

            if (materials.isEmpty()) {
                LOG.warn("No course material files found, unable to build index");
                return;
            }

            // Call index method to build index
            indexer.index(materials);
            updateMaterialsCache(materials, cachePath); // Update cache
            LOG.info("RAG index build completed");
        } catch (Exception e) {
            LOG.error("Failed to build RAG index", e);
        }
    }

    /**
     * Collect course material files in the project
     * Example: Assume materials are in the "slides" folder under the sandbox root, supporting PDF and Text
     * 
     * @param project the current project instance
     * @return list of collected course materials
     */
    private List<CourseMaterial> collectCourseMaterials(Project project) {
        List<CourseMaterial> materials = new ArrayList<>();
        String basePath = project.getBasePath();
        LOG.info("Project base path: " + basePath);

        // use sandbox root first
        String sandboxRoot = System.getProperty("idea.home.path");
        LOG.info("Sandbox root path: " + sandboxRoot);
        File sandboxRootSlides = new File(sandboxRoot, "slides");
        if (sandboxRootSlides.exists() && sandboxRootSlides.isDirectory()) {
            LOG.info("Using sandbox root slides");
            collectFromDir(sandboxRootSlides, materials);
            if (!materials.isEmpty()) return materials;
        }

        // use plugins path if available
        String sandboxPath = System.getProperty("idea.plugins.path");
        if (sandboxPath != null) {
            File sandboxSlides = new File(sandboxPath, "JEDAI/slides");
            if (sandboxSlides.exists() && sandboxSlides.isDirectory()) {
                LOG.info("Using sandbox slides path: " + sandboxSlides.getAbsolutePath());
                collectFromDir(sandboxSlides, materials);
                if (!materials.isEmpty()) return materials;
            }
        }

        File baseSlides = new File(basePath, "slides");
        LOG.info("Fallback to base slides path: " + baseSlides.getAbsolutePath());
        collectFromDir(baseSlides, materials);

        LOG.info("Collected materials count: " + materials.size());
        return materials;
    }

    /**
     * Helper method to collect files from directory
     * 
     * @param dir the directory to search for files
     * @param materials the list to add collected materials to
     */
    private void collectFromDir(File dir, List<CourseMaterial> materials) {
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    String name = file.getName().toLowerCase();
                    CourseMaterial.MaterialType type = null;
                    if (name.endsWith(".pdf")) {
                        type = CourseMaterial.MaterialType.PDF;
                    } else if (name.endsWith(".txt")) {
                        type = CourseMaterial.MaterialType.TEXT;
                    }
                    if (type != null) {
                        materials.add(new CourseMaterial(file, type));
                    }
                }
            }
        }
    }

    /**
     * Check if materials have changed: Compare file list and last modified time
     * 
     * @param currentMaterials the current list of materials
     * @param cachePath the path to the cache file
     * @return true if materials have changed, false otherwise
     * @throws IOException if there is an error reading the cache file
     */
    private boolean materialsChanged(List<CourseMaterial> currentMaterials, Path cachePath) throws IOException {
        if (!Files.exists(cachePath)) {
            return true; // No cache, considered as changed
        }
        String json = Files.readString(cachePath);
        Gson gson = new Gson();
        @SuppressWarnings("unchecked")
        Map<String, Long> cached = gson.fromJson(json, HashMap.class);

        Map<String, Long> currentMap = new HashMap<>();
        for (CourseMaterial mat : currentMaterials) {
            String filePath = mat.getFile().getAbsolutePath();
            long lastModified = mat.getFile().lastModified();
            currentMap.put(filePath, lastModified);
        }

        return !currentMap.equals(cached);
    }

    /**
     * Update material cache
     * 
     * @param materials the list of materials to cache
     * @param cachePath the path to the cache file
     * @throws IOException if there is an error writing the cache file
     */
    private void updateMaterialsCache(List<CourseMaterial> materials, Path cachePath) throws IOException {
        Map<String, Long> cacheMap = new HashMap<>();
        for (CourseMaterial mat : materials) {
            String filePath = mat.getFile().getAbsolutePath();
            long lastModified = mat.getFile().lastModified();
            cacheMap.put(filePath, lastModified);
        }
        Gson gson = new Gson();
        Files.writeString(cachePath, gson.toJson(cacheMap));
    }
}