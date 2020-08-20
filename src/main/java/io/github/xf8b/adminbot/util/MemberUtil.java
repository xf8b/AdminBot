package io.github.xf8b.adminbot.util;

import discord4j.core.object.entity.Member;
import lombok.experimental.UtilityClass;

@UtilityClass
public class MemberUtil {
    public String getTagWithDisplayName(Member member) {
        return member.getDisplayName() + "#" + member.getDiscriminator();
    }
}
