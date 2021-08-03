# OldMCPatcher
Minecraft 1.0~1.5.2 patcher.  
Look at https://kusaanko.github.io/OldMCPatcher/  

Minecraftの公式ランチャー2.0以降でMinecraft 1.0～1.5.2を問題なく遊ぶようにするためのパッチです。  

# 機能
* ゲームディレクトリ変更パッチ

通常の起動方式ではうまくゲームディレクトリが変更されないバージョンが存在します。  
そこでゲームディレクトリの情報をOldMCPatcherが受け取り、Minecraft側に適用します。

* スキン適用パッチ

Minecraft 1.0~1.5.2と現在ではスキンの画像が存在するURLが違うため、うまくスキンが適用されません。そこで、正しいURLを使用するように変更し、正しいスキンが使用できるようにします。この機能はマルチプレイに対応しています。

* 起動方式変更パッチ

公式ランチャー1.6以降ではMinecraft 1.0~1.5.2を起動する方法が公式ランチャー1.6以前とは違います。そのため、一部の旧ランチャーの起動方式用に作られたMODは動作しません。それらのMODが動作するようにするため、旧ランチャーと同様の起動方式に変更します。

# 使用方法
**このページでは高度なユーザー向けの解説を行います。Minecraft 1.0~1.5.2へのMOD導入を専用ツールなしに行え、公式ランチャー1.6以降で専用のツールなしにMOD導入が行え、jsonの読み書きができるユーザーが対象です。**  

**一般ユーザー向けには自動アップデート機能、自動導入機能、MOD導入サポート機能のついた[MCAddToJar](https://github.com/kusaanko/MCAddToJar)が利用可能です。**

jar導入型MODとほぼ同じです。一部jsonを書き換える必要があります。  
## 1. json書き換え
jsonファイルの中には使用する依存関係やアセットの情報、Minecraftのjarファイルに関するデータが記入されています。このままでは書き換えたjarファイルがバニラのものに置換されてまうのでそれに関する情報を消す必要があります。  

```json
    "downloads": {
        "client": {
            "sha1": "4a2fac7504182a97dcbcd7560c6392d7c8139928",
            "size": 4032098,
            "url": "https://launcher.mojang.com/v1/objects/4a2fac7504182a97dcbcd7560c6392d7c8139928/client.jar"
        },
        "server": {
            "sha1": "d8321edc9470e56b8ad5c67bbd16beba25843336",
            "size": 1408470,
            "url": "https://launcher.mojang.com/v1/objects/d8321edc9470e56b8ad5c67bbd16beba25843336/server.jar"
        },
        "windows_server": {
            "sha1": "8eaf5909489d9b54fd9748ddbbb4b6870a1d3de6",
            "size": 1589718,
            "url": "https://launcher.mojang.com/v1/objects/8eaf5909489d9b54fd9748ddbbb4b6870a1d3de6/windows_server.exe"
        }
    },
```
こういった項目があります。これがMinecraft本体のjarファイルのハッシュ値などが書かれており、改造を検知するとバニラをダウンロードする仕組みになっています。  
この部分をまるまる消すことでバニラに置換されなくなります。

```json
    "assetIndex": {
        "id": "pre-1.6",
        "sha1": "4759bad2824e419da9db32861fcdc3a274336532",
        "size": 73813,
        "totalSize": 49381897,
        "url": "https://launchermeta.mojang.com/v1/packages/4759bad2824e419da9db32861fcdc3a274336532/pre-1.6.json"
    },
    "assets": "pre-1.6",
```
assetIndex、assetsは最新のものにしておいてください。以前はlegacy.jsonを使用していましたが新たに作られたpre-1.6.jsonは正常に音がなるようになっています。

```json
    "mainClass": "net.minecraft.launchwrapper.Launch",
```
ここを次のように変更してください。

```json
    "mainClass": "net.minecraft.client.OldMCPatcher.Main",
```
これが一番重要です。これをしないとパッチは当たりません。

## 2.OldMCPatcherをjarファイルにコピー
7-Zipなどを使用してOldMCPatcherの中身をMinecraft本体のjarにコピーしてください。  
忘れずにMETA-INFは消しましょう。

# 仕組み
公式ランチャーが使用するメインクラスをjsonファイルに書いておくことができることを利用して自作のクラスをメインクラスに設定することで自作のクラスを呼び出してくれるようになります。  
これで割り込んだりする必要がなくなります。さらにMinecraftのコードを書き換えたりする必要がなくなります。  
あとは旧ランチャーと同じ起動方式で起動するだけです。  
スキンパッチに関してはバニラとForgeの環境でテクスチャ関係が違うのでForge環境かどうかを判定する仕組みを入れ、それに合わせてスキンを適用させています。  
ゲームディレクトリはリフレクションを使用し、ゲームディレクトリが書かれている変数に代入しています。作業ディレクトリはもとからゲームディレクトリと同じようなので問題ありません。  