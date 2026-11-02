package com.parca16.lupus.game;

/** Golpe del Caballero al morir. */
public record KnightStrike(String knightId, String targetId, HitResult result) {
}
