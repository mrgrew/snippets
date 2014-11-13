import java.io.File;
import java.io.IOException;
import java.util.Properties;

import javax.naming.NamingException;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.jndi.SimpleNamingContextBuilder;

/**
 * Utility class for test cases that use LDAP and JPA and
 * therefore require a JNDI container be available.  This
 * class sets up a simple JNDI container for such use.
 *
 * Properties are loaded from a file named TestUtils.properties
 * in the user's home directory as determined by the user.home
 * system property.  Property names with a ".jndiName" suffix
 * and property value that start with "jdbc" are trimmed of
 * that suffix to determine a base name to set up a
 * DriverManagerDataSource using the properties 
 * <ul>
 *   <li>{base name}.driverClassName</li>
 *   <li>{base name}.url</li>
 *   <li>{base name}.username</li>
 *   <li>{base name}.password</li>
 * </ul>
 * 
 * Data sources must be defined before the
 * Spring applicationContext is loaded, so the 
 * recommended use is to call getInstance() during the
 * setUpBeforeClass method in JUnit 4 test cases.
 *
 * Unfortunately the JNDI container provided by this
 * class is not compatible with LDAP so the container
 * must be de-activated using the deactivateContextBuilder
 * method before running any LDAP tests. 
 *
 * Feel free to add any other needed properties for
 * testing to the TestUtils.properties file, they are
 * available by calling the getTestProperties()
 * method.
 *
 * @author mrgrew
 */
public final class TestUtils
{
	private static TestUtils instance;

	public static synchronized TestUtils getInstance() throws IOException, NamingException
	{
		if ( instance == null )
		{
			instance = new TestUtils();
		}
		return instance;
	}

	private SimpleNamingContextBuilder builder;
	private Properties                 testProperties = new Properties();

	// No public instances allowed
	private TestUtils() throws IOException, NamingException
	{
		File propsFile = new File( System.getProperty( "user.home" ), "TestUtils.properties" );
		PropertiesLoaderUtils.fillProperties(testProperties, new FileSystemResource( propsFile ) ) ;

		builder = SimpleNamingContextBuilder.emptyActivatedContextBuilder();

		handleJndiNames();
	}
	
	private void handleJndiNames()
	{
		for ( String name : testProperties.stringPropertyNames() ) 
		{
			if ( name.endsWith( ".jndiName" ) )
			{
				// Trim off .jndiName from name for base
				String base = name.substring( 0, name.length() - 9 );
				String prop = testProperties.getProperty( name );

				if ( prop.startsWith ( "jdbc/" ) )
				{
					DriverManagerDataSource dmds = new DriverManagerDataSource();
					dmds.setDriverClassName( testProperties.getProperty( base + ".driverClassName" ) );
					dmds.setUrl( testProperties.getProperty( base + ".url" ) );
					dmds.setUsername( testProperties.getProperty( base + ".username" ) );
					dmds.setPassword( testProperties.getProperty( base + ".password" ) );

					builder.bind("java:comp/env/" + prop, dmds);
				}
			}
		}
	}

	public void activateContextBuilder() throws NamingException
	{
		builder.activate();
	}

	public void deactivateContextBuilder()
	{
		builder.deactivate();
	}

	public SimpleNamingContextBuilder getContextBuilder()
	{
		return builder;
	}
	
	public Properties getTestProperties()
	{
		return testProperties;
	}

}
