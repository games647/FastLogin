package com.github.games647.fastlogin.core.importer;

public enum ImportPlugin {

    AUTO_IN(AutoInImporter.class),
    
    BPA(BPAImporter.class),

    ELDZI(ElDziAuthImporter.class);

    private final Class<? extends Importer> importerClass;

    ImportPlugin(Class<? extends Importer> importer) {
        this.importerClass = importer;
    }

    public Class<? extends Importer> getImporter() {
        return importerClass;
    }
}
