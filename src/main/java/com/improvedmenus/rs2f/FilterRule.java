package com.improvedmenus.rs2f;
import com.improvedmenus.rs2f.ast.Condition;
import lombok.Value;
@Value public class FilterRule { Condition cond; DisplayConfig display; boolean terminal; int sourceLine; }
