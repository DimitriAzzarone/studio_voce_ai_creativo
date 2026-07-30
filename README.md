# Studio Voce AI

Prima base Android nativa del progetto **Studio Voce AI**, realizzata con Kotlin, Jetpack Compose e Material 3.

## Funzioni presenti

- interfaccia Android nativa in tema scuro;
- identità visiva con sfondo blu molto scuro, pannelli blu notte e colore ciano principale;
- logo musicale, nome **Studio Voce AI** e sottotitolo **Musica · Spartito · Karaoke**;
- selezione reale di documenti audio con `ActivityResultContracts.OpenDocument`;
- richiesta del permesso persistente di lettura quando il provider lo consente;
- conservazione di URI e metadati durante le normali ricreazioni dell'Activity;
- visualizzazione di nome, tipo MIME e dimensione leggibile del file;
- rimozione esplicita della selezione;
- pannelli di stato per elaborazione, partitura, separazione e karaoke;
- modelli dati Kotlin iniziali per note, tempo e risultato della futura partitura;
- contratto `MusicXmlExporter` con risultato controllato `NotImplemented`;
- workflow GitHub Actions con Java 17, Gradle 8.9 e APK debug negli Artifacts.

L'app usa URI del sistema Android e non tenta di ricavare percorsi filesystem diretti.

## Funzioni non implementate

- separazione AI di voce e accompagnamento;
- isolamento professionale della voce;
- trascrizione con Whisper;
- riconoscimento affidabile di melodia o accordi;
- generazione reale o professionale della partitura;
- esportazione MusicXML effettiva;
- modalità karaoke, sincronizzazione del testo ed esportazioni audio o grafiche.

I pannelli dell'interfaccia mostrano esclusivamente stati reali e non simulano analisi, note, accordi o avanzamenti.

## Limiti reali

La selezione del file audio è funzionante, ma il contenuto non viene ancora elaborato. Il permesso persistente dipende dal provider di documenti scelto: alcuni provider possono negarlo, nel qual caso l'app segnala il limite senza interrompersi.

La predisposizione della partitura consiste soltanto in modelli dati e interfacce compilabili. Non produce note inventate e non genera MusicXML finché non sarà implementato un motore reale.

Il progetto non usa server locali, Python sul telefono, chiavi API o modelli AI inclusi nell'APK.

## Base tecnica

- Kotlin 2.0.21
- Android Gradle Plugin 8.7.3
- Gradle 8.9 nel workflow GitHub Actions
- Jetpack Compose con Compose BOM 2024.12.01
- Material 3
- Java 17
- compileSdk 35
- targetSdk 35
- minSdk 26

## Gradle Wrapper

Il repository non include ancora il Gradle Wrapper completo (`gradlew`, `gradlew.bat`, `gradle-wrapper.jar` e `gradle-wrapper.properties`). Non vengono creati wrapper incompleti o falsi. GitHub Actions installa ed esegue **Gradle 8.9** tramite `gradle/actions/setup-gradle`.

## Compilazione e Artifact

La verifica principale avviene tramite **GitHub Actions**. Il workflow esegue:

```text
gradle projects --stacktrace
gradle assembleDebug --stacktrace
```

Una build può essere considerata riuscita solo quando GitHub Actions mostra esito positivo e l'APK è realmente disponibile nell'Artifact:

```text
Studio-Voce-AI-debug-<nome-ramo>
```

Percorso dell'APK caricato nell'Artifact:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Roadmap

1. Verificare la prima build Android e l'Artifact APK.
2. Valutare un modello audio specifico e ottimizzato per Android per separare voce e accompagnamento.
3. Aggiungere ascolto e salvataggio delle tracce realmente elaborate.
4. Valutare un modello tipo Whisper per la trascrizione.
5. Implementare analisi melodica e ritmica reale prima della generazione della partitura.
6. Implementare esportazione MusicXML, immagini e PDF soltanto a partire da risultati musicali reali.
7. Aggiungere progressivamente testo sincronizzato e modalità karaoke.
