# Fabric Mod Geliştirme Kuralları (26.1.x)
# Geliştirici: Cukkoo | com.cukkoo.MODID

---

## 0. Davranış Kuralları

- **Her görev bittiğinde** şunu açıkla: Ne kadar sürdü, neden bu kadar sürdü, hangi adım gereksiz uzadı, bir dahaki seferde nasıl daha hızlı yapılabilir.
- **Uzun süren her adımdan sonra** kısa bir özet ver: "Bu adım X dakika sürdü çünkü Y. Bir dahaki seferde Z yapacağım."
- **Hata ayıklarken** önce tüm hataları oku, sonra tek seferde düzelt — her hata için ayrı build çalıştırma.
- **Bir şeyi bilmiyorsan** hemen söyle, uzun araştırma döngüsüne girme.

---

## 1. KRİTİK: Mappings

Minecraft 26.1.x'te kod **obfuscated değil**, isimler zaten okunabilir.
`build.gradle` içinde **mappings satırı OLMAMALI**.

❌ YANLIŞ:
```groovy
mappings loom.officialMojangMappings()
mappings "net.fabricmc:yarn:..."
```

✅ DOĞRU: mappings satırı yok, hiç ekleme.

---

## 2. settings.gradle (zorunlu, eksik olursa Loom bulunamaz)

```groovy
pluginManagement {
    repositories {
        maven {
            name = 'Fabric'
            url = 'https://maven.fabricmc.net/'
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
```

---

## 3. build.gradle (çalışan şablon)

```groovy
plugins {
    id 'fabric-loom' version '1.15-SNAPSHOT'
    id 'maven-publish'
}

version = project.mod_version
group = project.maven_group

base {
    archivesName = project.archives_base_name
}

repositories {
    mavenCentral()
    maven { url = "https://maven.terraformersmc.com/releases/" }
    maven { url = "https://maven.shedaniel.me/" }
}

dependencies {
    minecraft "com.mojang:minecraft:${project.minecraft_version}"
    // mappings SATIRI YOK - 26.1.x obfuscated degil
    modImplementation "net.fabricmc:fabric-loader:${project.loader_version}"
    modImplementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_api_version}"
    // opsiyonel:
    // modCompileOnly "com.terraformersmc:modmenu:latest.release"
    // modCompileOnly "me.shedaniel.cloth:cloth-config-fabric:latest.release"
}

processResources {
    inputs.property "version", project.version
    filteringCharset = "UTF-8"
    filesMatching("fabric.mod.json") {
        expand "version": project.version
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
    withSourcesJar()
}

jar {
    from("LICENSE") {
        rename { "${it}_${project.archives_base_name}" }
    }
}
```

---

## 4. gradle.properties (çalışan şablon)

```properties
org.gradle.jvmargs=-Xmx2G
org.gradle.parallel=true
org.gradle.configuration-cache=false

minecraft_version=26.1.2
loader_version=0.18.5
fabric_api_version=0.145.5+26.1.2

mod_version=1.0.0
maven_group=com.cukkoo.MODID
archives_base_name=MODID
```

Gradle wrapper: `9.4.0`
Loom: `1.15-SNAPSHOT`
Java: `25`

---

## 5. fabric.mod.json (tam şablon)

```json
{
  "schemaVersion": 1,
  "id": "modid",
  "version": "${version}",
  "name": "Mod Adı",
  "description": "Açıklama.",
  "authors": ["Cukkoo"],
  "contact": {
    "sources": "https://github.com/Cukkoo12/REPO"
  },
  "license": "MIT",
  "icon": "assets/modid/icon.png",
  "environment": "client",
  "entrypoints": {
    "client": ["com.cukkoo.modid.ModAdi"]
  },
  "mixins": ["modid.mixins.json"],
  "depends": {
    "fabricloader": ">=0.18.5",
    "fabric-api": "*",
    "minecraft": ">=26.1.0"
  },
  "suggests": {
    "modmenu": "*",
    "cloth-config2": "*"
  },
  "compatibilityLevel": "JAVA_25"
}
```

**Notlar:**
- `"environment": "client"` → client-only mod
- `"environment": "*"` → hem client hem server
- `suggests` → opsiyonel bağımlılıklar buraya
- `compatibilityLevel: JAVA_25` zorunlu, eksik olursa hata

---

## 6. modid.mixins.json (şablon)

```json
{
  "required": true,
  "package": "com.cukkoo.modid.mixin",
  "compatibilityLevel": "JAVA_25",
  "mixins": [],
  "client": [
    "BirinciMixin",
    "IkinciMixin"
  ],
  "injectors": {
    "defaultRequire": 1
  }
}
```

**Notlar:**
- Server-side mixin → `"mixins"` listesine
- Client-side mixin → `"client"` listesine
- `defaultRequire: 1` → mixin hedef bulamazsa build hatası verir

---

## 7. Mixin şablonu

```java
package com.cukkoo.modid.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class BirinciMixin {

    @Inject(at = @At("HEAD"), method = "hedefMetod")
    private void hedefMetodBasinda(CallbackInfo ci) {
        // kod buraya
    }

    @Inject(at = @At("RETURN"), method = "hedefMetod")
    private void hedefMetodSonunda(CallbackInfo ci) {
        // kod buraya
    }
}
```

**Sık kullanılan At değerleri:**
- `"HEAD"` → metodun başında
- `"RETURN"` → metodun sonunda
- `"TAIL"` → son return'den önce

---

## 8. 26.1.x API Değişiklikleri

| Eski (1.21.x) | Yeni (26.1.x) |
|---------------|---------------|
| `new Identifier("ns", "path")` | `Identifier.of("ns", "path")` |
| `END_WORLD_TICK` | `END_SERVER_TICK` |
| `new ResourceLocation(...)` | `ResourceLocation.fromNamespaceAndPath(...)` |
| `level.getRandom()` | `protected` — doğrudan erişim yok |
| `getToastComponent()` | `getToastManager()` |
| `GuiGraphics` | Yok — `GuiGraphicsExtractor` kullan |
| `context.drawString(...)` | `extractor.text(...)` |
| `context.drawTextWithShadow(...)` | `extractor.text(...)` |
| `disconnect()V` | `disconnectWithSavingScreen()V` |
| `saveAll(ZZZ)V` | `saveAllChunks(ZZZ)Z` — dönüş tipi boolean! |
| `mouseClicked(DDI)Z` | `mouseClicked(MouseButtonEvent, boolean)Z` |
| `render(GuiGraphics, ...)` | `extractRenderState(GuiGraphicsExtractor, ...)` — `extractContent` değil! |
| `MinecraftClient` | `Minecraft` |
| `Text.translatable(...)` | `Component.translatable(...)` |
| `TextRenderer` | `Font` |
| `client.textRenderer` | `client.font` |
| `textRenderer.getWidth(...)` | `font.width(...)` |
| `IntegratedServer` (server import) | `net.minecraft.client.server.IntegratedServer` |
| `TooltipType` | `TooltipFlag` (`net.minecraft.world.item.TooltipFlag`) |
| `modImplementation` | `implementation` (26.1.x'te mod remapping yok) |
| `modCompileOnly` | `compileOnly` |
| `getWorlds()` | `getAllLevels()` |
| `getSaveProperties().getLevelName()` | `getWorldData().getLevelName()` |
| `level.getName()` | `summary.getLevelId()` |
| `Screen` import | `net.minecraft.client.gui.screens.Screen` |
| `net.minecraft.client.renderer.GuiGraphicsExtractor` | `net.minecraft.client.gui.GuiGraphicsExtractor` — paket farklı! |
| `Inventory.selected` (field) | `inventory.getSelectedSlot()` — field private |
| `inventory.items` | `inventory.getNonEquipmentItems()` |
| `pose().pushPose/popPose` | `Matrix3x2fStack` → `pushMatrix/popMatrix` |
| `pose().translate(x, y, z)` | `pose.translate(x, y)` — 2D, z parametresi yok |
| `pose().scale(x, y, z)` | `pose.scale(x, y)` — 2D, z parametresi yok |
| `slot.getSlotIndex()` | `slot.getContainerSlot()` |
| `client.getWindow().getGuiScaledWidth()` | `extractor.guiWidth()` |
| `client.getWindow().getGuiScaledHeight()` | `extractor.guiHeight()` |
| Gui render: `render(GuiGraphicsExtractor, float)` | `extractRenderState(GuiGraphicsExtractor, DeltaTracker)` |
| `TextureAtlasSprite.uvShrinkRatio()` | Kaldırıldı — `getU(float)` / `getV(float)` intercept et |
| `BlockElement` | `CuboidModelElement` (`net.minecraft.client.resources.model.cuboid`, immutable record) |
| `BlockElementFace` | `CuboidFace` (aynı paket, immutable record) |
| `ItemModelGenerator` (eski paket) | `net.minecraft.client.resources.model.cuboid.ItemModelGenerator` |
| `ItemModelGenerator.createSideElements()` | Kaldırıldı — `geometry()` intercept et, `UnbakedCuboidGeometry` döner |
| `BlockElement.from` / `BlockElement.to` (mutable) | `CuboidModelElement.from()` / `to()` (getter, record) — modify için yeni instance lazım |
| `TextureAtlas.atlasLocation()` | Yok — `location()` kullan; sprite'ta `atlasLocation()` var |
| `com.mojang.math.Vector3f` | `org.joml.Vector3f` (26.1'de JOML'e geçildi) |

### Kritik Java kuralı — Lambda effectively final:
Lambda içinde kullanılan değişken yeniden atanmışsa hata verir:
```java
// YANLIS:
String worldName = foo();
if (worldName == null) worldName = "default"; // yeniden atama
thread.start(() -> use(worldName)); // HATA

// DOGRU:
String wn = foo();
if (wn == null) wn = "default";
final String worldName = wn; // yeni final değişken
thread.start(() -> use(worldName)); // OK
```

### Doğru Fabric API versiyonu:
`fabric_api_version=0.145.4+26.1.2` — 0.145.5 yok, en yüksek bu!
`fabric.mod.json` içinde de `"fabric-api": ">=0.145.4"` yaz, 0.145.5 yazarsa oyun başlamaz.

### Plugin ID:
`id 'net.fabricmc.fabric-loom' version '1.15-SNAPSHOT'` — bare `fabric-loom` değil, 26.1.x için `net.fabricmc.fabric-loom` gerekli.

### compatibilityLevel:
`"compatibilityLevel": "JAVA_25"` sadece **mixins json**'da geçerli — `fabric.mod.json`'a ekleme, uyarı verir.
Bu metod içinde kaydetme ekranı + bekleme döngüsü var, direkt iptal edilmeli:
```java
@Inject(method = "disconnectWithSavingScreen()V", at = @At("HEAD"), cancellable = true)
private void onDisconnectWithSavingScreen(CallbackInfo ci) {
    if (!hasSingleplayerServer()) return;
    // ... arka plan save başlat ...
    server.saveAllChunks(false, true, true); // MinecraftServerMixin yakalar
    singleplayerServer = null; // sunucuyu null'la, bekleme döngüsü atlanır
    ci.cancel(); // orijinal metodu iptal et
    disconnect(new TitleScreen(), true, true); // direkt title screen
}
```

### Thread güvenliği — saveAllChunks:
`server.saveAllChunks()` **render thread'den çağrılmamalı** — server thread'le çakışır, crash olur.
Bunun yerine sunucunun doğal kapanış save'ini yakala:

```java
// YANLIŞ - render thread'den çağırma:
server.saveAllChunks(false, true, true); // CRASH

// DOGRU - sunucunun kendi save'ini yakala:
@Inject(method = "saveAllChunks(ZZZ)Z", at = @At("HEAD"))
private void onSaveStart(...) {
    if (!InstantQuit.pendingSaveTracking) return;
    InstantQuit.WORLD_SAVE_MANAGER.startSaving(worldName);
    InstantQuit.activeSavingWorld = worldName;
}

@Inject(method = "saveAllChunks(ZZZ)Z", at = @At("RETURN"))
private void onSaveEnd(...) {
    String worldName = InstantQuit.activeSavingWorld;
    if (worldName == null) return;
    InstantQuit.activeSavingWorld = null;
    InstantQuit.onBackgroundSaveComplete(worldName);
}
```

### Tab listesi arkaplanını genişletmek:
`@ModifyConstant` MixinExtras 0.5.3'te yok — kullanma.
`@Redirect` on JDK metotları (Math.min gibi) güvenilir değil — kullanma.
Doğru yol: `@ModifyArgs` ile `fill()` çağrısının x2 parametresini değiştir:

```java
@ModifyArgs(method = "extractRenderState",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V"))
private void extendBackground(Args args) {
    int x2 = args.get(2);
    args.set(2, x2 + 15); // sağa 15px uzat
}
```

### Tab listesi ping:
- `PlayerTabOverlay` → `net.minecraft.client.gui.components.PlayerTabOverlay`
- Ping icon metodu: `extractPingIcon(GuiGraphicsExtractor, int a, int b, int c, PlayerInfo)`
  - Bar X = `a + b - 11`, Bar Y = `c`
- Ping değeri: `playerInfo.getLatency()` → ms cinsinden int
Yeni dünyada save süresi 0.0s göstermesi normaldir — kaydedilecek chunk yok.

---

## 9. Opsiyonel Cloth Config + Mod Menu entegrasyonu

### Çalışma anında kontrol:
```java
boolean hasClothConfig = FabricLoader.getInstance().isModLoaded("cloth-config2");
boolean hasModMenu = FabricLoader.getInstance().isModLoaded("modmenu");
```

### fabric.mod.json entrypoint:
```json
"entrypoints": {
    "client": ["com.cukkoo.modid.ModAdi"],
    "modmenu": ["com.cukkoo.modid.ModMenuIntegration"]
}
```

### ModMenuIntegration.java:
```java
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        if (!FabricLoader.getInstance().isModLoaded("cloth-config2")) return null;
        return parent -> ModConfigScreen.create(parent);
    }
}
```

---

## 10. Config sistemi (Cloth Config olmadan, sade JSON)

```java
public class ModConfig {
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
        .getConfigDir().resolve("modid.json");

    public boolean secenek1 = true;
    public int secenek2 = 3;

    public static ModConfig load() {
        try {
            if (Files.exists(CONFIG_PATH))
                return new Gson().fromJson(Files.readString(CONFIG_PATH), ModConfig.class);
        } catch (Exception e) { /* ignore */ }
        return new ModConfig();
    }

    public void save() {
        try {
            Files.writeString(CONFIG_PATH,
                new GsonBuilder().setPrettyPrinting().create().toJson(this));
        } catch (Exception e) { /* ignore */ }
    }
}
```

---

## 11. Modrinth Yükleme Checklist

- [ ] Mod ID küçük harf, alt çizgi veya tire — boşluk yok
- [ ] Versiyon formatı: `1.0.0`
- [ ] Desteklenen MC versiyonları doğru seçildi
- [ ] Loader: Fabric seçildi
- [ ] Lisans: MIT
- [ ] icon.png var (128x128 önerilen)
- [ ] Açıklama sayfası dolu
- [ ] `fabric.mod.json` depends bloğu doğru

---

## 12. Git Branch Stratejisi

```
main              → kararlı, en son sürüm
snapshot/26.2-x   → snapshot portları
port/1.21.11      → eski versiyon portları
port/1.21.4       → eski versiyon portları
port/1.20.1       → eski versiyon portları
```

Yeni port:
```bash
git checkout main
git checkout -b port/1.21.11
# gradle.properties güncelle
# API değişikliklerini düzelt
git commit -m "Port to 1.21.11"
```

---

## 13. Verimli Çalışma Kuralları

- **Kod yazmadan önce** Mixin hedef sınıflarını javap ile kontrol et — 30 saniye alır, 15 dakika kazandırır:
  ```bash
  jar_path=".gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-*/26.1.2/minecraft-merged-*.jar"
  javap -public -cp $jar_path net.minecraft.client.gui.Gui
  javap -public -cp $jar_path net.minecraft.world.entity.player.Inventory
  ```
- **Minecraft jar yolu:** `.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-*/26.1.2/minecraft-merged-*.jar` — başka yerde arama
- **Sınıf/metod adı bilinmiyorsa:** `javap -public -cp <jar> <ClassName>` ile tek seferde tüm gerekli sınıfları kontrol et, her sınıf için ayrı komut çalıştırma
- **Toplu değişiklik:** Aynı değişkeni birden fazla yerde değiştiriyorsan tüm dosyayı tek seferde yeniden yaz, 6 ayrı edit yapma
- **Cache kilidi hatası:** `rm -rf .gradle/loom-cache` ile temizle, tekrar dene
- **Build hatası çıkınca:** Tüm hata çıktısını oku, her hatayı tek seferde düzelt, tek tek derleme yapma

## 15. Çalışan Kod Şablonları

### 15.1 HUD Hotbar Mixin (GuiMixin)
Hotbar üzerine bir şey çizmek için:

```java
package com.cukkoo.modid.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V",
            at = @At("RETURN"))
    private void afterExtractRenderState(GuiGraphicsExtractor extractor, DeltaTracker tracker, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        int screenWidth = extractor.guiWidth();
        int screenHeight = extractor.guiHeight();
        int hotbarStartX = screenWidth / 2 - 91;
        int hotbarY = screenHeight - 22;

        for (int i = 0; i <= 8; i++) {
            int slotX = hotbarStartX + i * 20;
            // slotX, hotbarY konumuna çiz
            Matrix3x2fStack pose = extractor.pose();
            pose.pushMatrix();
            pose.translate(slotX + 11, hotbarY + 3);
            extractor.text(client.font, "✦", 0, 0, 0xFFFFD700);
            pose.popMatrix();
        }
    }
}
```

---

### 15.2 Inventory Screen Mixin (AbstractContainerScreenMixin)
Envanter ekranındaki slotlara bir şey çizmek için:

```java
package com.cukkoo.modid.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public class AbstractContainerScreenMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
            at = @At("RETURN"))
    private void afterExtractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY,
                                         float delta, CallbackInfo ci) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        for (Slot slot : screen.getMenu().slots) {
            if (!(slot.container instanceof Inventory)) continue;
            int slotIndex = slot.getContainerSlot(); // getSlotIndex() değil!
            int slotX = slot.x;
            int slotY = slot.y;

            // slotX + 11, slotY + 1 konumuna çiz
            Matrix3x2fStack pose = extractor.pose();
            pose.pushMatrix();
            pose.translate(slotX + 11, slotY + 1);
            extractor.text(client.font, "✦", 0, 0, 0xFFFFD700);
            pose.popMatrix();
        }
    }
}
```

---

### 15.3 Tooltip Event (ItemTooltipCallback)
Item tooltip'ine satır eklemek için (mixin gerekmez):

```java
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag; // TooltipType değil!

// onInitializeClient içinde:
ItemTooltipCallback.EVENT.register(this::onItemTooltip);

// metod:
private void onItemTooltip(ItemStack stack, Item.TooltipContext context,
                            TooltipFlag type, java.util.List<Component> lines) {
    if (stack.isEmpty() || !stack.isDamageableItem()) return;
    int current = stack.getMaxDamage() - stack.getDamageValue();
    int max = stack.getMaxDamage();
    lines.add(Component.literal("Durability: " + current + " / " + max)
        .withStyle(ChatFormatting.GREEN));
}
```

---

### 15.4 disconnectWithSavingScreen Bypass (InstantQuit tarzı)
Kaydetme ekranını atlayıp anında title screen'e dönmek için:

```java
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.server.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftClientMixin {

    @Shadow public abstract boolean hasSingleplayerServer();
    @Shadow public abstract IntegratedServer getSingleplayerServer();
    @Shadow public abstract void disconnect(net.minecraft.client.gui.screens.Screen screen, boolean flag, boolean flag2);
    @Shadow private IntegratedServer singleplayerServer;

    @Inject(method = "disconnectWithSavingScreen()V", at = @At("HEAD"), cancellable = true)
    private void onDisconnectWithSavingScreen(CallbackInfo ci) {
        if (!hasSingleplayerServer()) return;
        IntegratedServer server = getSingleplayerServer();
        if (server == null) return;

        String worldName = server.getWorldData().getLevelName();
        // arka plan işlemlerini başlat...

        singleplayerServer = null; // bekleme döngüsünü atla
        ci.cancel();
        disconnect(new TitleScreen(), true, true); // anında title screen
    }
}
```

---

### 15.5 saveAllChunks Tracking (server save süresini ölçmek için)

```java
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {

    @Shadow public abstract PlayerList getPlayerList();
    @Shadow public abstract Iterable<ServerLevel> getAllLevels();

    @Inject(method = "saveAllChunks(ZZZ)Z", at = @At("HEAD"))
    private void onSaveStart(boolean suppressLogs, boolean flush, boolean force,
                              CallbackInfoReturnable<Boolean> cir) {
        // save başladı — süreç ölçümü başlat
    }

    @Inject(method = "saveAllChunks(ZZZ)Z", at = @At("RETURN"))
    private void onSaveEnd(boolean suppressLogs, boolean flush, boolean force,
                            CallbackInfoReturnable<Boolean> cir) {
        // save bitti — süreç ölçümü durdur
    }
}
```

---

### 15.6 Texture Atlas UV Fix (TextureAtlasSpriteMixin)
Block modellerindeki UV padding'i sıfırlamak için (No Model Gaps tarzı):

```java
@Mixin(TextureAtlasSprite.class)
public class TextureAtlasSpriteMixin {

    @Shadow private int padding;

    @Inject(method = "getU(F)F", at = @At("HEAD"), cancellable = true)
    private void modifyGetU(float u, CallbackInfoReturnable<Float> cir) {
        TextureAtlasSprite self = (TextureAtlasSprite) (Object) this;
        if (!self.atlasLocation().equals(TextureAtlas.LOCATION_BLOCKS)) return;

        float u0 = self.getU0();
        float u1 = self.getU1();
        int w = self.contents().width();
        if (w <= 2 * padding) return;

        float padFrac = (float) padding * (u1 - u0) / (w - 2 * padding);
        cir.setReturnValue((u0 - padFrac) + ((u1 + padFrac) - (u0 - padFrac)) * u);
    }

    @Inject(method = "getV(F)F", at = @At("HEAD"), cancellable = true)
    private void modifyGetV(float v, CallbackInfoReturnable<Float> cir) {
        TextureAtlasSprite self = (TextureAtlasSprite) (Object) this;
        if (!self.atlasLocation().equals(TextureAtlas.LOCATION_BLOCKS)) return;

        float v0 = self.getV0();
        float v1 = self.getV1();
        int h = self.contents().height();
        if (h <= 2 * padding) return;

        float padFrac = (float) padding * (v1 - v0) / (h - 2 * padding);
        cir.setReturnValue((v0 - padFrac) + ((v1 + padFrac) - (v0 - padFrac)) * v);
    }
}
```

### 15.7 Item Model Face Genişletme (ItemModelMixin)
Item modellerindeki boşlukları kapatmak için (`CuboidModelElement` immutable record olduğundan yeni instance lazım):

```java
@Mixin(ItemModelGenerator.class)
public class ItemModelMixin {

    private static final float EXPANSION = 0.002f;

    @Inject(method = "geometry()Lnet/minecraft/client/resources/model/geometry/UnbakedGeometry;",
            at = @At("RETURN"), cancellable = true)
    private void modifyGeometry(CallbackInfoReturnable<UnbakedGeometry> cir) {
        if (!(cir.getReturnValue() instanceof UnbakedCuboidGeometry cuboid)) return;

        List<CuboidModelElement> modified = new ArrayList<>();
        for (CuboidModelElement element : cuboid.elements()) {
            Map<Direction, CuboidFace> faces = element.faces();
            if (faces.size() != 1) { modified.add(element); continue; }

            Direction dir = faces.keySet().iterator().next();
            Vector3f from = new Vector3f(element.from());
            Vector3f to = new Vector3f(element.to());

            switch (dir) {
                case UP    -> to.add(0, EXPANSION, 0);
                case DOWN  -> from.sub(0, EXPANSION, 0);
                case EAST  -> to.add(EXPANSION, 0, 0);
                case WEST  -> from.sub(EXPANSION, 0, 0);
            }
            modified.add(new CuboidModelElement(from, to, faces,
                    element.rotation(), element.shade(), element.lightEmission()));
        }
        cir.setReturnValue(new UnbakedCuboidGeometry(modified));
    }
}
```

| Hata | Sebep | Çözüm |
|------|-------|-------|
| `Configuration 'mappings' has no dependencies` | mappings satırı var | mappings satırını sil |
| `Cannot use Mojang mappings in non-obfuscated environment` | mappings satırı var | mappings satırını sil |
| `class file major version 69` | Loom çok eski | Loom `1.15-SNAPSHOT` yap |
| `archivesBaseName` hatası | eski Gradle API | `archivesName` kullan |
| `modImplementation not found` | 26.1.x'te yok | `implementation` kullan |
| `Unsupported class file major version 69` | Loom Java 25 jar'ı okuyamıyor | Loom `1.15-SNAPSHOT` yap |
| `Found existing cache lock file` | önceki build yarım kaldı | `rm -rf .gradle/loom-cache` |
| `Cannot find symbol: TooltipType` | isim değişti | `TooltipFlag` kullan |
| `Cannot find symbol: GuiGraphics` | 26.1.x'te yok | `GuiGraphicsExtractor` kullan |
| `src refspec main does not match any` | branch adı master | `git push -u origin master` |
| `RPC failed; curl 55` | büyük dosya push edildi | `.gitignore` ekle, `git rm -r --cached build .gradle run` |
| `defaultRequire` mixin hatası | mixin hedef metod bulunamadı | javap ile metod imzasını kontrol et |

---

## 17. Fabric API Event Listesi (26.1.x)

```java
// Client tick
ClientTickEvents.END_CLIENT_TICK.register(client -> { });
ClientTickEvents.START_CLIENT_TICK.register(client -> { });

// Server tick
ServerTickEvents.END_SERVER_TICK.register(server -> { });

// Item tooltip
ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> { });
// import: net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
// type: net.minecraft.world.item.TooltipFlag (TooltipType değil!)

// Item pickup
EntityItemPickupCallback.EVENT.register((player, entity) -> { });
// import: net.fabricmc.fabric.api.event.player.EntityItemPickupCallback

// Screen events
ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> { });
ScreenEvents.BEFORE_RENDER.register((screen, extractor, mouseX, mouseY, delta) -> { });
// import: net.fabricmc.fabric.api.client.screen.v1.ScreenEvents

// Server lifecycle
ServerLifecycleEvents.SERVER_STARTED.register(server -> { });
ServerLifecycleEvents.SERVER_STOPPING.register(server -> { });

// World events
ServerWorldEvents.LOAD.register((server, world) -> { });
ServerWorldEvents.UNLOAD.register((server, world) -> { });

// Player events
ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> { });
```

---

## 18. Yaygın Import Listesi (26.1.x)

```java
// Temel Minecraft
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.client.player.LocalPlayer;

// Network/Chat
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.ChatFormatting;

// World/Server
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.PlayerList;

// Items
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;

// Resources
import net.minecraft.resources.Identifier; // Identifier.of() kullan
import net.minecraft.world.level.storage.LevelSummary;

// Math
import org.joml.Matrix3x2fStack;
import org.joml.Vector3f;
import org.joml.Vector3fc;

// Cuboid Model (26.1.x - texture/model fix için)
import net.minecraft.client.resources.model.cuboid.CuboidModelElement;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.cuboid.ItemModelGenerator;
import net.minecraft.client.resources.model.cuboid.UnbakedCuboidGeometry;

// Fabric
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.loader.api.FabricLoader;

// Mixin
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Config
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
```

---

## 19. Build Hızlandırma

`gradle.properties`'e ekle:

```properties
org.gradle.daemon=true
org.gradle.caching=true
org.gradle.parallel=true
org.gradle.configureondemand=true
```

İlk build yavaş olur, sonrakiler çok hızlanır. Cache sayesinde değişmeyen task'lar tekrar çalışmaz.

---

## 20. Paket ve Dosya Yapısı

```
src/main/java/com/cukkoo/modid/
├── ModAdi.java                      # Ana mod sınıfı (ClientModInitializer)
├── config/
│   └── ModConfig.java               # Config sistemi
├── mixin/
│   └── HedefMixin.java              # Mixin sınıfları
└── integration/
    └── ModMenuIntegration.java      # Opsiyonel

src/main/resources/
├── fabric.mod.json
├── modid.mixins.json
└── assets/modid/
    ├── icon.png
    └── lang/
        └── en_us.json
```

---

## 21. 1.21.4 Fabric Modu (Yarn Mappings)

### 21.1 gradle.properties (1.21.4)

```properties
org.gradle.jvmargs=-Xmx2G
org.gradle.parallel=true
org.gradle.configuration-cache=false
org.gradle.daemon=true
org.gradle.caching=true

minecraft_version=1.21.4
yarn_mappings=1.21.4+build.8
loader_version=0.19.2
fabric_api_version=0.119.4+1.21.4

mod_version=1.0.0
maven_group=com.cukkoo.MODID
archives_base_name=MODID
```

Gradle wrapper: `9.4.x` | Loom: `1.9-SNAPSHOT` | Java: `21`

### 21.2 build.gradle (1.21.4)

```groovy
plugins {
    id 'fabric-loom' version '1.9-SNAPSHOT'
    id 'maven-publish'
}

version = project.mod_version
group = project.maven_group

base {
    archivesName = project.archives_base_name
}

dependencies {
    minecraft "com.mojang:minecraft:${project.minecraft_version}"
    mappings "net.fabricmc:yarn:${project.yarn_mappings}:v2"
    modImplementation "net.fabricmc:fabric-loader:${project.loader_version}"
    modImplementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_api_version}"
}

processResources {
    inputs.property "version", project.version
    filteringCharset = "UTF-8"
    filesMatching("fabric.mod.json") {
        expand "version": project.version
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    withSourcesJar()
}
```

### 21.3 fabric.mod.json (1.21.4)

```json
{
  "schemaVersion": 1,
  "id": "modid",
  "version": "${version}",
  "name": "Mod Adı",
  "description": "Açıklama.",
  "authors": ["Cukkoo"],
  "license": "MIT",
  "environment": "*",
  "entrypoints": {
    "main": ["com.cukkoo.modid.ModAdi"],
    "client": ["com.cukkoo.modid.ModAdi"]
  },
  "mixins": ["modid.mixins.json"],
  "depends": {
    "fabricloader": ">=0.16.9",
    "fabric-api": ">=0.115.0",
    "minecraft": "~1.21.4"
  }
}
```

**Notlar:**
- `compatibilityLevel` fabric.mod.json'da OLMAMALI — sadece mixins.json'da olmalı
- `environment: "*"` hem client hem server, `"client"` sadece client
- Her iki entrypoint aynı sınıfı implement etmeli: `ModInitializer` + `ClientModInitializer`

### 21.4 mixins.json (1.21.4)

```json
{
  "required": true,
  "package": "com.cukkoo.modid.mixin",
  "compatibilityLevel": "JAVA_21",
  "mixins": ["ServerSideMixin"],
  "client": ["ClientSideMixin"],
  "injectors": {
    "defaultRequire": 1
  }
}
```

---

### 21.5 1.21.4 API Farkları (26.1.x → 1.21.4 Yarn)

| 26.1.x | 1.21.4 Yarn |
|--------|-------------|
| `Minecraft` | `MinecraftClient` |
| `ServerLevel` | `ServerWorld` |
| `ServerPlayer` | `ServerPlayerEntity` |
| `ServerPlayerGameMode` | `ServerPlayerInteractionManager` |
| `Gui` | `InGameHud` (paket: `net.minecraft.client.gui.hud`) |
| `GuiGraphicsExtractor` | `DrawContext` |
| `DeltaTracker` | `RenderTickCounter` |
| `Matrix3x2fStack` | `MatrixStack` |
| `Component.translatable(...)` | `Text.translatable(...)` |
| `ChatFormatting` | `Formatting` |
| `ResultSlot` | `CraftingResultSlot` |
| `onTake(Player, ItemStack)` | `onTakeItem(PlayerEntity, ItemStack)` |
| `destroyBlock / tryBreakBlock` | `finishMining(pos, seq, reason)` — blok kırma için bu! |
| `startSleepInBed(pos)` | `trySleep(pos)` |
| `Player.BedSleepingProblem` | `PlayerEntity.SleepFailureReason` |
| `Level.ExplosionInteraction.BLOCK` | `World.ExplosionSourceType.BLOCK` |
| `level.explode(...)` | `world.createExplosion(...)` |
| `LightLayer.BLOCK/SKY` | `LightType.BLOCK/SKY` |
| `LevelLightEngine` | `LightingProvider` |
| `getLayerListener(LightLayer)` | `get(LightType)` → `ChunkLightingView` |
| `DataLayer` | `ChunkNibbleArray` |
| `SectionPos` | `ChunkSectionPos` |
| `hasLightWork()` | `hasUpdates()` |
| `runLightUpdates()` | `doLightUpdates()` |
| `updateSectionStatus(pos, flag)` | `setSectionStatus(pos, notReady)` |
| `setLightEnabled(pos, flag)` | `setColumnEnabled(pos, lightEnabled)` |
| `propagateLightSources(pos)` | `propagateLight(pos)` |
| `SkyRenderer` | `SkyRendering` (net.minecraft.client.render) |
| `renderSkyDisc` | `renderSky(float r, float g, float b)` |
| `renderSunMoonAndStars` | `renderCelestialBodies(...)` |
| `ItemModelGenerator` (eski paket) | Yok — 1.21.4'te farklı sistem |
| `BlockElement` | Yok — 1.21.4'te farklı sistem |
| `player.sendSystemMessage(comp)` | `player.sendMessage(text, false)` |
| `player.kill()` | `player.kill(serverWorld)` — ServerWorld parametre lazım |
| `Block.getDescriptionId()` | `block.getRegistryEntry().isIn(BlockTag)` veya `block.getName()` |
| `BuiltInRegistries.BLOCK.getKey(block)` | Yok — `BlockTags` kullan |
| `level.destroyBlock(pos, drop)` | `world.breakBlock(pos, drop, entity, flags)` |
| `SoundEvents.AMBIENT_CAVE` | `SoundEvents.AMBIENT_CAVE.value()` (Holder.Reference) |
| `server.createCommandSourceStack()` | `server.getCommandSource()` |
| `server.getCommands().performPrefixedCommand(...)` | `server.getCommandManager().executeWithPrefix(...)` |
| `overworld.getLevelData().getGameTime()` | `overworld.getLevelProperties().getTime()` |
| `server.getPlayerList().getPlayers()` | `server.getPlayerManager().getPlayerList()` |
| `server.overworld()` | `server.getOverworld()` |
| `chunk.getPos()` | `chunk.getPos()` (aynı) |
| `chunkMap.forEachReadyToSendChunk(...)` | `chunkLoadingManager` üzerinden — doğrudan çalışmıyor |
| `client.levelRenderer` | `client.worldRenderer` |
| `client.levelRenderer.allChanged()` | `client.worldRenderer.reload()` |
| `getSaveProperties().getLevelName()` | `getSaveProperties().getLevelName()` (aynı) |

---

### 21.6 Entity Rendering Sistemi (1.21.4)

1.21.4'te entity rendering 3 tip parametreli sisteme geçti: `Entity`, `RenderState`, `Model`.

```java
// Renderer: 3 tip parametreli
public class MyEntityRenderer
        extends MobEntityRenderer<MyEntity, MyEntityRenderState, MyEntityModel> {

    public MyEntityRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new MyEntityModel(ctx.getPart(MY_MODEL_LAYER)), 0.5f);
    }

    @Override
    public MyEntityRenderState createRenderState() {
        return new MyEntityRenderState();
    }

    @Override
    public void updateRenderState(MyEntity entity, MyEntityRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
        state.myField = entity.getMyField(); // entity'den state'e veri aktar
    }

    @Override
    public void render(MyEntityRenderState state, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        // state üzerinden render et
    }

    @Override
    public Identifier getTexture(MyEntityRenderState state) {
        return TEXTURE;
    }
}

// RenderState: entity verisi burada taşınır
public class MyEntityRenderState extends LivingEntityRenderState {
    public int myField;
}

// Model: LivingEntityRenderState (veya custom) ile çalışır
public class MyEntityModel extends EntityModel<MyEntityRenderState> {
    public MyEntityModel(ModelPart root) {
        super(root);
    }

    @Override
    public void setAngles(MyEntityRenderState state) {
        super.setAngles(state);
        // state.limbFrequency, state.limbAmplitudeMultiplier, state.age kullan
        // state.yawDegrees, state.pitch kullan
    }
}
```

**Model Layer Kaydı:**
```java
// Ana sınıfta:
EntityRendererRegistry.register(MyEntities.MY_ENTITY, MyEntityRenderer::new);
EntityModelLayerRegistry.registerModelLayer(MY_MODEL_LAYER, MyEntityModel::getTexturedModelData);

// Model Layer tanımı:
public static final EntityModelLayer MY_MODEL_LAYER =
        new EntityModelLayer(Identifier.of("modid", "my_entity"), "main");
```

**Entity Type Kaydı (1.21.4):**
```java
private static final RegistryKey<EntityType<?>> MY_KEY = RegistryKey.of(
        RegistryKeys.ENTITY_TYPE, Identifier.of("modid", "my_entity"));

public static final EntityType<MyEntity> MY_ENTITY = Registry.register(
        Registries.ENTITY_TYPE,
        MY_KEY,
        EntityType.Builder.create(MyEntity::new, SpawnGroup.MISC)
                .dimensions(0.7f, 2.4f)
                .maxTrackingRange(64)
                .build(MY_KEY)
);

// Attribute kaydı:
public static void register() {
    FabricDefaultAttributeRegistry.register(MY_ENTITY, MyEntity.createAttributes());
}
```

---

### 21.7 1.21.4 Mixin Örnekleri

**SkyRendering — Gökyüzünü siyah yap:**
```java
@Mixin(SkyRendering.class)
public class SkyRendererMixin {
    private boolean nosun$inside; // re-entry guard

    @Inject(method = "renderSky(FFF)V", at = @At("HEAD"), cancellable = true)
    private void onRenderSky(float r, float g, float b, CallbackInfo ci) {
        if (nosun$inside || !isActive()) return;
        ci.cancel();
        nosun$inside = true;
        try {
            ((SkyRendering) (Object) this).renderSky(0f, 0f, 0f); // siyah
        } finally {
            nosun$inside = false;
        }
    }

    @Inject(method = "renderCelestialBodies", at = @At("HEAD"), cancellable = true)
    private void onCelestials(MatrixStack m, VertexConsumerProvider.Immediate v,
                               float t, int i, float f, float g, Fog fog, CallbackInfo ci) {
        if (isActive()) ci.cancel(); // güneş/ay/yıldız yok
    }
}
```

**LightingProvider — Sky ışığını sıfırla:**
```java
@Mixin(LightingProvider.class)
public class LightingProviderMixin {
    @Inject(method = "get", at = @At("RETURN"), cancellable = true)
    private void onGet(LightType type, CallbackInfoReturnable<ChunkLightingView> cir) {
        if (type != LightType.SKY || !isFullDarkness()) return;
        ChunkLightingView original = cir.getReturnValue();
        if (original == null || original instanceof DarkSkyView) return;
        cir.setReturnValue(new DarkSkyView(original));
    }

    private static class DarkSkyView implements ChunkLightingView {
        private final ChunkLightingView delegate;
        DarkSkyView(ChunkLightingView d) { this.delegate = d; }

        @Override public int getLightLevel(BlockPos pos) { return 0; }
        @Override public ChunkNibbleArray getLightSection(ChunkSectionPos p) { return delegate.getLightSection(p); }
        @Override public boolean hasUpdates() { return delegate.hasUpdates(); }
        @Override public int doLightUpdates() { return delegate.doLightUpdates(); }
        @Override public void setSectionStatus(ChunkSectionPos p, boolean b) { delegate.setSectionStatus(p, b); }
        @Override public void setColumnEnabled(ChunkPos p, boolean b) { delegate.setColumnEnabled(p, b); }
        @Override public void propagateLight(ChunkPos p) { delegate.propagateLight(p); }
    }
}
```

**InGameHud — HUD çizimi:**
```java
@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Inject(method = "render", at = @At("RETURN"))
    private void afterRender(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        int w = context.getScaledWindowWidth();
        int h = context.getScaledWindowHeight();
        // DrawContext.fill(x1, y1, x2, y2, argbColor)
        // DrawContext.drawText(textRenderer, text, x, y, color, shadow)
        // MatrixStack: context.getMatrices().push/pop/translate
    }
}
```

**ServerPlayerInteractionManager — Blok kırma takibi:**
```java
@Mixin(ServerPlayerInteractionManager.class)
public class BlockBreakMixin {
    @Shadow protected ServerWorld world;

    // finishMining: survival'da asıl blok kırma buradan geçer
    @Inject(method = "finishMining", at = @At("HEAD"))
    private void onFinishMining(BlockPos pos, int sequence, String reason, CallbackInfo ci) {
        if (world == null || pos == null) return;
        Block block = world.getBlockState(pos).getBlock();
        // blok kırma eventi burada
    }
}
```

**Yatak patlatma (trySleep):**
```java
@Mixin(ServerPlayerEntity.class)
public class BedMixin {
    @Inject(method = "trySleep", at = @At("HEAD"), cancellable = true)
    private void onTrySleep(BlockPos pos,
                             CallbackInfoReturnable<Either<PlayerEntity.SleepFailureReason, Unit>> cir) {
        if (!isActive()) return;
        ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
        if (self.getWorld().isClient) return;
        self.getWorld().createExplosion(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                3.0f, World.ExplosionSourceType.BLOCK);
        cir.setReturnValue(Either.left(PlayerEntity.SleepFailureReason.OTHER_PROBLEM));
    }
}
```

---

### 21.8 1.21.4 Import Listesi (Yarn)

```java
// Client
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;

// Server
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;

// World/Block
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.light.LightingProvider;
import net.minecraft.world.chunk.light.ChunkLightingView;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.registry.tag.BlockTags;

// Entity
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;

// Entity Rendering
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.RenderLayer;

// Text/Sound
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;

// Math/Util
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;

// Registry
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;

// Rendering
import net.minecraft.client.render.SkyRendering;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Fog;
import net.minecraft.client.render.FogShape;

// Screen slot
import net.minecraft.screen.slot.CraftingResultSlot;
```

---

### 21.9 1.21.4 Hata Kataloğu

| Hata | Sebep | Çözüm |
|------|-------|-------|
| `Mixin has no targets` | Sınıf adı yanlış | javap ile doğru sınıfı bul |
| `tryBreakBlock` inject çalışmıyor | Survival'da finishMining çağrılır | `finishMining` metodunu intercept et |
| `player.kill()` compile hatası | 1.21.4'te ServerWorld parametre lazım | `player.kill(player.getServerWorld())` |
| `SoundEvents.AMBIENT_CAVE` tip hatası | Holder.Reference tipinde | `.value()` ekle |
| `renderSky` infinite recursion | Self çağırıyor | Re-entry guard (boolean flag) ekle |
| `compatibilityLevel` fabric.mod.json'da | Sadece mixins.json'da olmalı | fabric.mod.json'dan kaldır |
| `build()` EntityType hata | String parametre değil key lazım | `build(REGISTRY_KEY)` kullan |
| `getDescriptionId()` yok | 1.21.4'te bu metod yok | `getRegistryEntry().isIn(BlockTags.X)` kullan |
| Entity görünmüyor | Texture yüklenmiyor / WHISPER fazı | Renderer'da faz kontrolü yap, texture PNG'yi doğrula |
