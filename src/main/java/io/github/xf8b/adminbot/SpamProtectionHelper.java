package io.github.xf8b.adminbot;

import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.MessageChannel;
import lombok.experimental.UtilityClass;

@UtilityClass
public class SpamProtectionHelper {
    //private final Multimap<Snowflake, Snowflake> WARNS = Multimaps.synchronizedListMultimap(ArrayListMultimap.create());
    //private final ScheduledExecutorService WARNS_SCHEDULED_EXECUTOR = Executors.newScheduledThreadPool(1);

    public void checkForSpam(Guild guild, Member member, MessageChannel channel, Member selfMember) {
        /*
        String guildId = member.getGuildId().asString();
        String userId = member.getId().asString();
        long epochSecond = Instant.now().getEpochSecond();
        channel.getMessagesBefore(Snowflake.of(Instant.now()))
                .filter(message -> !message.getContent().isEmpty())
                .filter(message -> message.getAuthorAsMember().blockOptional().isPresent())
                .filter(message -> message.getAuthor().isPresent())
                .take(5)
                .all(message -> message.getAuthor().get().getId().equals(member.getId()) &&
                        message.getTimestamp().getEpochSecond() < 5 + epochSecond)
                .flatMap(isSpamming -> {
                    if (WARNS.containsKey(member.getId())) return Mono.empty();
                    if (isSpamming) {
                        WARNS.put(member.getId(), null);
                        WARNS_SCHEDULED_EXECUTOR.schedule(() -> WARNS.remove(member.getId(), null),
                                5,
                                TimeUnit.SECONDS
                        );
                        String warnReason = "Spam in " + channel.getMention();
                        try {
                            if (WarnsDatabaseHelper.doesUserHaveWarn(guildId, userId, warnReason)) {
                                List<String> warnIds = new ArrayList<>();
                                WarnsDatabaseHelper.getAllWarnsForUser(guildId, userId).forEach((reasonInDatabase, warnId) -> {
                                    if (reasonInDatabase.equals(warnReason)) {
                                        warnIds.add(warnId);
                                    }
                                });
                                Collections.reverse(warnIds);
                                String top = warnIds.get(0);
                                String warnId = String.valueOf(Integer.parseInt(top) + 1);
                                WarnsDatabaseHelper.insertIntoWarns(guildId, userId, warnId, warnReason);
                            } else {
                                WarnsDatabaseHelper.insertIntoWarns(guildId, userId, String.valueOf(0), warnReason);
                            }
                        } catch (ClassNotFoundException | SQLException exception) {
                            exception.printStackTrace();
                        }
                        return member.getPrivateChannel()
                                .flatMap(privateChannel -> {
                                    if (member.isBot()) {
                                        return Mono.empty();
                                    } else if (member == selfMember) {
                                        return Mono.empty();
                                    } else {
                                        return Mono.just(privateChannel);
                                    }
                                })
                                .flatMap(privateChannel -> privateChannel
                                        .createEmbed(embedCreateSpec -> embedCreateSpec.setTitle("You were warned!")
                                                .setFooter("Warned by: " + MemberUtil.getTagWithDisplayName(selfMember), selfMember.getAvatarUrl())
                                                .addField("Server", guild.getName(), false)
                                                .addField("Reason", warnReason, false)
                                                .setTimestamp(Instant.now())
                                                .setColor(Color.RED))
                                        .onErrorResume(ClientExceptionUtil.isClientExceptionWithCode(50007), throwable -> Mono.empty())); //cannot send to user
                    } else {
                        return Mono.empty();
                    }
                })
                .subscribe();
                */
    }
}
