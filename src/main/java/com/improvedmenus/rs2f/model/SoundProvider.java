package com.improvedmenus.rs2f.model;
import com.improvedmenus.rs2f.lang.Token;
/** Validate the source value but never execute audio as part of icon evaluation. */
public final class SoundProvider {
 public static SoundProvider fromExpr(Token t) { if(t.getType()==Token.Type.LITERAL_INT) { t.expectInt(); } else { t.expectString(); } return new SoundProvider(); }
}
