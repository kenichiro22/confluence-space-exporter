import groovy.net.xmlrpc.*;

/**
 * Confluenceのドキュメントの更新有無を確認して、更新されていればPDFとHTMLエクスポートして
 * 指定したスペースに添付ファイルとしてアップロードするスクリプト
 *
 * Copyright (c) 2010 by Kenichiro Tanaka
 */

if(args.size() < 3){
	println "Usage: SpaceExporter.groovy ConfluenceURL username password timeSpan(option)"
	return
}

baseUrl = args[0]
username = args[1]
password = args[2]

println "Confluence: ${baseUrl}"


// 何日以内に更新があればエクスポートを実行するか？
// (cronやHudsonの実行間隔とあわせる)
timeSpan = args.size() == 4 ? args[3] : 1

println "timeSpan: ${timeSpan}"


// エクスポートしたファイルを添付するスペースとページ
docSpaceKey = 'DOCS'
docSpacePage = "Home"

// ドキュメントスペースのラベル
docLabels = ['html_docs', 'pdf_docs']

c = new XMLRPCServerProxy(baseUrl + "/rpc/xmlrpc")

c.confluence1.login(username, password)  {token ->
    // docsラベルの付いたスペースを取得
    docLabels.each{label->
		println "Checking label: ${label}..."

    	c.confluence1.getSpacesWithLabel(token, label).each{ space ->
			println "Checking space: ${space.name}"

			if(isSpaceModified(space.key)){
	    		// 変更があればダウンロードして添付ファイルに追加
	    		def type = label.substring(0, label.indexOf('_'))
	    		println "Export ${space.name} to ${type}"
	    		exportSpaceAndUpload(token, space, type)
	    	}
    	}
    }
}

/**
 * 指定したスペースをエクスポートしてアップロードする。
 *
 * @param token
 * @param space
 * @param type
 * @return
 */
def exportSpaceAndUpload(token, space, type){
    if(type == 'pdf')
        u = c.pdfexport.exportSpace(token, space.key)
    else if(type == 'html')
        u = c.confluence1.exportSpace(token, space.key, "TYPE_HTML")

    attach(token, download(u, space.name))
}

/**
 * 指定したファイルをドキュメント管理用スペースにアップロードする
 *
 * @param token
 * @param file
 * @return
 */
def attach(token, file){
    def page = c.confluence1.getPage(token, docSpaceKey, docSpacePage)

    def bout = new ByteArrayOutputStream()
    def fin = new FileInputStream(file);

    bout << fin

    def attachment = [
        pageId: page.id,
        fileName: file.name ,
        contentType: file.name.endsWith('.pdf') ? 'application/pdf' : 'application/octet-stream',
        comment: 'Document updated.'
    ]

    c.confluence1.addAttachment(token, page.id, attachment, bout.toByteArray())
    fin.close()
    bout.close()
}

/**
 * 指定したURLファイルをダウンロードする
 *
 * @param address ダウンロード対象URL
 * @param filename 保存する際のファイル名
 * @return ダウンロードしたファイル
 */
def download(address, filename)
{
    def file = new File(System.getProperty("java.io.tmpdir"),  filename + "." + address.tokenize(".")[-1])

    def fin = new FileOutputStream(file)
    def out = new BufferedOutputStream(fin)

    out << new URL(withAuthentication(address)).openStream()
    out.close()

    println("\tdownloaded: ${address}")
    return file
}

/**
 * 指定されたスペースが更新されたかを確認する
 *
 * @param spaceKey
 * @return
 */
def isSpaceModified(spaceKey){
	def feedUrl = "${baseUrl}/createrssfeed.action?types=page&pageSubTypes=attachment&spaces=${spaceKey}&sort=modified&maxResults=1&timeSpan=${timeSpan}&confirm=Create&showContent=false&showDiff=false&os_authType=basic"
    return new XmlParser().parse(withAuthentication(feedUrl)).entry.size() > 0
}

/**
 * URLに認証用のパラメータを付加する
 *
 * @param url
 * @return
 */
def withAuthentication(url){
    url += url.indexOf('?') == -1 ? '?' : '&'
    return url + "os_username=${username}&os_password=${password}"
}


