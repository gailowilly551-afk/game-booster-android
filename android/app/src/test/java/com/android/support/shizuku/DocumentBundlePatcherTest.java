package com.android.support.shizuku;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class DocumentBundlePatcherTest {

    @Test
    public void resolveDocumentPaths_defaultsToMlbbWhenEmpty() {
        List<String> paths = DocumentBundlePatcher.resolveDocumentPaths("");
        assertFalse(paths.isEmpty());
        assertTrue(paths.stream().anyMatch(p -> p.contains("com.mobile.legends")));
        assertTrue(paths.stream().anyMatch(p -> p.endsWith("/Document.unity3d")));
        assertTrue(paths.stream().anyMatch(p -> p.endsWith("/GameSettings_Rank.json")));
    }

    @Test
    public void resolveDocumentPaths_usesCustomPackageName() {
        List<String> paths = DocumentBundlePatcher.resolveDocumentPaths("com.mobile.legends.vn");
        assertTrue(paths.stream().allMatch(p -> p.contains("com.mobile.legends.vn")));
    }
}

