
c(
	bibentry(bibtype = "Manual",
		title = "${model.getConfig().getTitle()}",
		year = ${model.getConfig().getYear()},
		note = "R package version ${model.getConfig().getVersion()}",
		author = c(
		<#list model.getAuthors() as author>
			person(given = "${author[0]}",<#if author[1]??>family = "${author[1]}",</#if><#if author[2]??>email = "${author[2]}",</#if><#if author[3]??>comment = structure("${author[3]}", .Names = "ORCID"),</#if>role = <#if author?is_first>c("aut", "cre")<#else>"aut"</#if>)<#sep>,
		</#list>
		)
	),
	bibentry(bibtype = "Manual",
		title = "R6 generator maven plugin",
		author = person(given="Rob", family="Challen",role="aut",email="rc538@exeter.ac.uk",comment = structure("0000-0002-5504-7768", .Names = "ORCID")),
		note = "Maven plugin",
		year = 2020,
		url = "https://github.com/terminological/r6-generator-maven-plugin"
	)
)