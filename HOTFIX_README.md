# Serverless fallback compile hotfix — 26.1.2

Apply this ZIP **on top of Test 2** (PF-EMI-26.1.2-v1.1-Discovery-Serverless-Issue6-Test2.zip). Overwrite the existing `EmiClientRecipeFallback.java` while preserving the path. It does **not** alter your version number or any other source file.

Fixes the compiler error at line 69 by replacing the unavailable `LocalPlayer.displayClientMessage(...)` call with the supported `mc.gui.getChat().addClientSystemMessage(...)` method.

Not an independently complete mod patch: it assumes the previous Test 2 is installed.
