package org.autojs.autojs.core.shizuku;

interface IUserService {
    void destroy();
    void exit();
    String execCommand(String command);
    String currentPackage();
    String currentActivity();
    String currentComponent();
    String currentComponentShort();
}