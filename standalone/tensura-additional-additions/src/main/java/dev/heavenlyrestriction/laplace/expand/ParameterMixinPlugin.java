package dev.heavenlyrestriction.laplace.expand;

import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.extensibility.*;

/** Adds stack-neutral field-read hooks. Never changes shared configuration objects. */
public final class ParameterMixinPlugin implements IMixinConfigPlugin {
  public void onLoad(String mixinPackage) {}

  public String getRefMapperConfig() {
    return null;
  }

  public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
    return true;
  }

  public void acceptTargets(Set<String> mine, Set<String> other) {}

  public List<String> getMixins() {
    return null;
  }

  public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}

  public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {
    if (!mixin.endsWith("ParameterReadMixin")) return;
    ParameterTransformer.apply(node);
  }
}
