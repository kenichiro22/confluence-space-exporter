import groovy.net.xmlrpc.*;

/**
 * Confluence�̃h�L�������g�̍X�V�L�����m�F���āA�X�V����Ă����PDF��HTML�G�N�X�|�[�g����
 * �w�肵���X�y�[�X�ɓY�t�t�@�C���Ƃ��ăA�b�v���[�h����X�N���v�g
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


// �����ȓ��ɍX�V������΃G�N�X�|�[�g�����s���邩�H
// (cron��Hudson�̎��s�Ԋu�Ƃ��킹��)
timeSpan = args.size() == 4 ? args[3] : 1

println "timeSpan: ${timeSpan}"


// �G�N�X�|�[�g�����t�@�C����Y�t����X�y�[�X�ƃy�[�W
docSpaceKey = 'DOCS'
docSpacePage = "Home"

// �h�L�������g�X�y�[�X�̃��x��
docLabels = ['html_docs', 'pdf_docs']

c = new XMLRPCServerProxy(baseUrl + "/rpc/xmlrpc")

c.confluence1.login(username, password)  {token ->
    // docs���x���̕t�����X�y�[�X���擾
    docLabels.each{label->
		println "Checking label: ${label}..."

    	c.confluence1.getSpacesWithLabel(token, label).each{ space ->
			println "Checking space: ${space.name}"

			if(isSpaceModified(space.key)){
	    		// �ύX������΃_�E�����[�h���ēY�t�t�@�C���ɒǉ�
	    		def type = label.substring(0, label.indexOf('_'))
	    		println "Export ${space.name} to ${type}"
	    		exportSpaceAndUpload(token, space, type)
	    	}
    	}
    }
}

/**
 * �w�肵���X�y�[�X���G�N�X�|�[�g���ăA�b�v���[�h����B
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
 * �w�肵���t�@�C�����h�L�������g�Ǘ��p�X�y�[�X�ɃA�b�v���[�h����
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
 * �w�肵��URL�t�@�C�����_�E�����[�h����
 *
 * @param address �_�E�����[�h�Ώ�URL
 * @param filename �ۑ�����ۂ̃t�@�C����
 * @return �_�E�����[�h�����t�@�C��
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
 * �w�肳�ꂽ�X�y�[�X���X�V���ꂽ�����m�F����
 *
 * @param spaceKey
 * @return
 */
def isSpaceModified(spaceKey){
	def feedUrl = "${baseUrl}/createrssfeed.action?types=page&pageSubTypes=attachment&spaces=${spaceKey}&sort=modified&maxResults=1&timeSpan=${timeSpan}&confirm=Create&showContent=false&showDiff=false&os_authType=basic"
    return new XmlParser().parse(withAuthentication(feedUrl)).entry.size() > 0
}

/**
 * URL�ɔF�ؗp�̃p�����[�^��t������
 *
 * @param url
 * @return
 */
def withAuthentication(url){
    url += url.indexOf('?') == -1 ? '?' : '&'
    return url + "os_username=${username}&os_password=${password}"
}


