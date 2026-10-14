package com.gamebooster.app.engine;

import com.gamebooster.app.mods.GameModEngine;
import com.gamebooster.app.mods.codm.CodmModManager;
import com.gamebooster.app.mods.mlbb.MlbbModManager;

import org.junit.Test;

import static org.junit.Assert.*;

public class NonRootedEngineTest {

    @Test
    public void testUnwrapSuCommandsForShizuku() {
        assertEquals("settings put system peak_refresh_rate 120",
                PrivilegeBridgeEngine.unwrapSuCommand("su -c 'settings put system peak_refresh_rate 120'"));

        assertEquals("settings put system min_refresh_rate 144",
                PrivilegeBridgeEngine.unwrapSuCommand("su -c \"settings put system min_refresh_rate 144\""));

        assertEquals("setprop debug.sf.fps 165",
                PrivilegeBridgeEngine.unwrapSuCommand("sudo setprop debug.sf.fps 165"));

        assertEquals("sh", PrivilegeBridgeEngine.unwrapSuCommand("su"));
        assertEquals("sh", PrivilegeBridgeEngine.unwrapSuCommand("su -"));
        assertEquals("sh", PrivilegeBridgeEngine.unwrapSuCommand("su root"));

        assertEquals("getprop ro.product.model",
                PrivilegeBridgeEngine.unwrapSuCommand("getprop ro.product.model"));

        assertEquals("", PrivilegeBridgeEngine.unwrapSuCommand(null));
        assertEquals("", PrivilegeBridgeEngine.unwrapSuCommand("   "));
    }

    @Test
    public void testCommandExecutorSuccessEvaluation() {
        assertTrue(CommandExecutor.isSuccessOutput("SUCCESS"));
        assertTrue(CommandExecutor.isSuccessOutput(""));
        assertTrue(CommandExecutor.isSuccessOutput("   "));
        assertTrue(CommandExecutor.isSuccessOutput("120.0"));
        assertTrue(CommandExecutor.isSuccessOutput("SM-S928B"));

        assertFalse(CommandExecutor.isSuccessOutput("ERROR: No elevated channel available"));
        assertFalse(CommandExecutor.isSuccessOutput("Permission Denial: cannot write secure settings"));
        assertFalse(CommandExecutor.isSuccessOutput("java.lang.SecurityException: not allowed"));
        assertFalse(CommandExecutor.isSuccessOutput("Operation not permitted"));
        assertFalse(CommandExecutor.isSuccessOutput("/system/bin/sh: su: not found"));
        assertFalse(CommandExecutor.isSuccessOutput("Command failed with exit code 1"));
    }

    @Test
    public void testExecuteSystemCommandGracefulNonRootExecution() {
        // In JVM test environment, ShellExecutor.isAndroidEnvironment() is false,
        // so executeSystemCommand returns "SUCCESS" without throwing any su or shell IOException.
        String result = CommandExecutor.executeSystemCommand("setprop gamebooster.test.flag 1");
        assertNotNull("Command output must not be null", result);
        assertFalse("Should not throw unhandled exception or return null", result.isEmpty());
    }

    @Test
    public void testModManagersSafeNullProfileHandling() {
        assertFalse("injectMlbbSync with null profile must return false safely",
                GameModEngine.injectMlbbSync(null, "com.mobile.legends", null));

        assertFalse("injectCodmSync with null profile must return false safely",
                GameModEngine.injectCodmSync(null, "com.garena.game.codm", null));

        assertFalse("injectMlbbModSuite with null profile must return false safely",
                MlbbModManager.injectMlbbModSuite(null, "com.mobile.legends", null));

        assertFalse("injectCodmModSuite with null profile must return false safely",
                CodmModManager.injectCodmModSuite(null, "com.garena.game.codm", null));
    }

    @Test
    public void testModManagersStateLifecycle() {
        MlbbModManager.resetInjectionState();
        assertFalse(MlbbModManager.isMlbbInjected());

        CodmModManager.resetInjectionState();
        assertFalse(CodmModManager.isCodmInjected());
    }
}
