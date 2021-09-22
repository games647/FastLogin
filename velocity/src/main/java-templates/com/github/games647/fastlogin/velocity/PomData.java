package com.github.games647.fastlogin.velocity;

public class PomData {
    public static final String DISPLAY_NAME = "${project.name}";
    public static final String NAME = "${project.parent.artifactId}";
    public static final String VERSION = "${project.version}-${git.commit.id.abbrev}";
    public static final String DESCRIPTION = "${project.parent.description}";
    public static final String URL = "${project.parent.url}";
}
