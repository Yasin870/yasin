# Custom Menu (custommenu)
Minecraft menülerini kendi fotoğraf, video, GIF ve müziklerinle kişiselleştir!

İkon ChatGPT tarafından tasarlandı (assets/ klasöründe).

## Durum (dürüst tablo)
| Parça | Durum |
|---|---|
| Ortak çekirdek (common/): ayarlar, kayıt, klasör tarama, dosya kopyalama | Yazıldı, derlenmedi |
| Forge 1.20.1: PNG/JPG gösterme (ana menü, ESC, game over), arka planda yükleme | Yazıldı, TEST EDİLMEDİ |
| Ayar ekranı (mod + 4 ekran), kendi dosya yöneticisi (gezinme, arama, boyut, kopyalama) | Forge 1.20.1'de yazıldı, TEST EDİLMEDİ |
| Küçük önizlemeler, sistem dosya seçicisi (SAF) | Yok |
| GIF animasyonu, MP3 çalma, MP4 oynatma | Yok (dosya seçilip kaydedilir ama oynatılmaz) |
| Fabric 1.20.1, NeoForge 1.21.1, Fabric 1.21.5, Fabric 26.3 | Sadece mod bilgisi + ikon |
| JAR | Yok (GitHub Actions ile derlenebilir: .github/workflows) |

Bilinen sorun: ana menüde panorama resmin üstüne çizilebilir; düzeltmek için Mixin gerekebilir.
