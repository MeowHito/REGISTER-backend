package com.actionth.membership.config;

import java.util.Arrays;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.actionth.membership.auth.ApplicationUserService;
import com.actionth.membership.auth.FilterChainExceptionHandler;
import com.actionth.membership.jwt.JWTTokenVerifier;
import com.actionth.membership.jwt.JWTUsernameAndPasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

	private static final Logger log = LoggerFactory.getLogger(SecurityConfiguration.class);

	private final PasswordEncoder passwordEncoder;
	private final ApplicationUserService applicationUserService;
	private final FilterChainExceptionHandler filterChainExceptionHandler;

	@Value("${jwt.secret-key}")
	private String jwtSecretKey;

	@Value("${app.env}")
	private String appEnv;

	private static final String[] PUBLIC_URLS = {
			"/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**",
			"/swagger-resources/**", "/webjars/**",
			"/public-api/**",
			"/login",
			"/actuator/health",
			"/api/order/validate-token/**",
			"/api/orderHistory/detail/**",
			"/api/coupon/getCoupons/**",
			"/api/coupon/validateCoupon",
			"/api/order/update",
			"/api/gatewaypayment/ewallet-qrcode",
			"/api/gatewaypayment/qrcode",
			"/api/gatewaypayment/verify-payment",
			"/api/webhook/partner-2c2p",
			"/api/webhook/scb",
			"/api/webhook/scb/get-payload",
			"/api/log/**",
	};

	public SecurityConfiguration(
			PasswordEncoder passwordEncoder,
			ApplicationUserService applicationUserService,
			FilterChainExceptionHandler filterChainExceptionHandler) {
		this.passwordEncoder = passwordEncoder;
		this.applicationUserService = applicationUserService;
		this.filterChainExceptionHandler = filterChainExceptionHandler;
	}

	@PostConstruct
	public void init() {
		log.info("=================================================");
		log.info("Security Configuration Initialized");
		log.info("Application Environment: {}", appEnv != null ? appEnv : "NOT SET");
		log.info("Is Production Mode: {}", "PROD".equalsIgnoreCase(appEnv));
		log.info("=================================================");
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationManager authenticationManager) throws Exception {
		JWTUsernameAndPasswordAuthenticationFilter authFilter = new JWTUsernameAndPasswordAuthenticationFilter(
				authenticationManager, jwtSecretKey, appEnv);
		authFilter.setFilterProcessesUrl("/login");

		JWTTokenVerifier tokenVerifier = new JWTTokenVerifier(jwtSecretKey);

		http
				.cors(cors -> {
				})
				.csrf(csrf -> csrf.disable())
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authenticationProvider(daoAuthenticationProvider())
				.addFilter(authFilter)
				.addFilterAfter(tokenVerifier, JWTUsernameAndPasswordAuthenticationFilter.class)
				.addFilterBefore(filterChainExceptionHandler, JWTUsernameAndPasswordAuthenticationFilter.class)
				.authorizeHttpRequests(auth -> auth
						.antMatchers(PUBLIC_URLS).permitAll()
						.antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						// .antMatchers("/backoffice/**").hasAnyRole("ADMIN", "ORGANIZER")
						// .antMatchers("/royalty/**").hasRole("GUEST")
						.anyRequest().authenticated())
				.logout(logout -> logout
						.logoutUrl("/logout")
						.clearAuthentication(true)
						.invalidateHttpSession(true)
						.deleteCookies("access_token", "JSESSIONID")
						.logoutSuccessHandler((req, res, auth) -> {
							boolean isProd = !"DEV".equalsIgnoreCase(Optional.ofNullable(appEnv).orElse("DEV"));

							var clearJwt = ResponseCookie.from("access_token", "")
									.httpOnly(true)
									.secure(isProd)
									.sameSite("Lax")
									.path("/")
									.maxAge(0)
									.build();

							var clearSession = ResponseCookie.from("JSESSIONID", "")
									.path("/")
									.maxAge(0)
									.build();

							res.addHeader(HttpHeaders.SET_COOKIE, clearJwt.toString());
							res.addHeader(HttpHeaders.SET_COOKIE, clearSession.toString());
							res.setContentType("application/json; charset=UTF-8");
							res.getWriter().write("{\"success\":true}");
						}));

		return http.build();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
		return authenticationConfiguration.getAuthenticationManager();
	}

	@Bean
	public DaoAuthenticationProvider daoAuthenticationProvider() {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
		provider.setPasswordEncoder(passwordEncoder);
		provider.setUserDetailsService(applicationUserService);
		return provider;
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration cfg = new CorsConfiguration();

		cfg.setAllowCredentials(true);

		if (appEnv == null || !"PROD".equalsIgnoreCase(appEnv)) {
			cfg.setAllowedOriginPatterns(Arrays.asList(
					"http://localhost:*",
					"http://127.0.0.1:*",
					"https://membership.testuiapp.com"));
		} else {
			cfg.setAllowedOriginPatterns(Arrays.asList(
					"https://register-action-in-thai.netlify.app", "https://register.action.in.th"));
		}

		cfg.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));

		cfg.setAllowedHeaders(Arrays.asList(
				"Authorization", "Content-Type", "Cache-Control",
				"Origin", "Accept", "X-Requested-With"));

		UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
		src.registerCorsConfiguration("/**", cfg);
		return src;
	}

}
