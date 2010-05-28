Confluenceのドキュメントの更新有無を確認して、PDF/HTMLにエクスポートし
指定したスペースのページにに添付ファイルとしてアップロードします。

使い方：
groovy SpaceExporter.groovy ConfluenceURL username password timeSpan

timeSpanは、何日以内に更新されていたらエクスポートし直すかを指定します。
（指定しないと1日以内になります）

"pdf_docs"もしくは"html_docs"というラベルを付けたのスペースが、
それぞれHTML/PDFへのエクスポート対象になります。

アップロードするスペースキーは'DOCS'、ページは'Home'になっています。


※ GroovyのXML-RPCライブラリが必要です。
http://groovy.codehaus.org/XMLRPC
