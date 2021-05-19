package br.com.caelum.camel;

import java.text.SimpleDateFormat;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;

import com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource;
import com.thoughtworks.xstream.XStream;

public class RotaHttpPollingNegociacoes {

	private static MysqlConnectionPoolDataSource criaDataSource() {
		MysqlConnectionPoolDataSource mysqlDs = new MysqlConnectionPoolDataSource();
		mysqlDs.setDatabaseName("camel");
		mysqlDs.setServerName("localhost");
		mysqlDs.setPort(3306);
		mysqlDs.setUser("root");
		mysqlDs.setPassword("root");
		return mysqlDs;
	}

	public static void main(String[] args) throws Exception {

		final XStream xstream = new XStream();
		xstream.alias("negociacao", Negociacao.class);
		
		SimpleRegistry registro = new SimpleRegistry();
		registro.put("mysql",criaDataSource());
		CamelContext context = new DefaultCamelContext(registro);
		context.addRoutes(new RouteBuilder() {

			@Override
			public void configure() throws Exception {

				from("timer://negociações?fixedRate=true&delay=1s&period=360s")
						.to("http4://argentumws-spring.herokuapp.com/negociacoes").convertBodyTo(String.class)
						.unmarshal(new org.apache.camel.dataformat.xstream.XStreamDataFormat(xstream))
						.split(body())
						.process(new Processor() {
							
							@Override
							public void process(Exchange exchange) throws Exception {
								Negociacao negociacao = exchange.getIn().getBody(Negociacao.class);
								exchange.setProperty("preco",negociacao.getPreco());
								exchange.setProperty("quantidade",negociacao.getQuantidade());
								String data = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss").format(negociacao.getData().getTime());
								exchange.setProperty("data", data);
							}
						}) 
						.setBody(simple("insert into negociacao(preco,quantidade, data) values (${property.preco}, ${property.quantidade}, '${property.data}')"))
						.log("${body}")
						.delay(1000)
						.to("jdbc:mysql");
						//.end();
//						.setHeader(Exchange.FILE_NAME, constant("negociacoes.xml"))
//				.to("file:saida2");
			}
		});

		context.start();
		Thread.sleep(2000);
		context.stop();

	}

}
