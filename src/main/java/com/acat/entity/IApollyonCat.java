package com.acat.entity;

/**
 * 亚小猫族通用标记接口（Boss 版 ApollyonCatEntity 与 仆从版 ApollyonCatServantEntity 都实现）。
 * ─────────────────────────────────────────────────────────────
 * 用途：
 *  1. 客户端渲染器（ApollyonCatRenderer）按此接口限定实体类型，Boss 与仆从共用同一套渲染；
 *  2. 三阶段领域里「另一只亚小猫不互拉」的判定按接口进行，避免 Boss/仆从互斥遗漏。
 * （客户端天空盒特效已移除，实体不再需要向客户端同步三阶段视觉开关位。）
 */
public interface IApollyonCat {
}
