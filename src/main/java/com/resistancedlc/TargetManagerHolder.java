package com.resistancedlc.targetesp;

/**
 * TargetManagerHolder — единый экземпляр TargetManager для всего мода.
 * Заменяет статические поля из главного класса.
 */
public final class TargetManagerHolder {

    public static final TargetManager MANAGER = new TargetManager();

    private TargetManagerHolder() {}
}