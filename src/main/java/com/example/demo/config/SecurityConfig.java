package com.example.demo.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.beans.factory.annotation.Autowired;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	// OAuth2AuthorizedClientServiceを注入
	@Autowired
	private OAuth2AuthorizedClientService authorizedClientService;

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http.csrf(Customizer.withDefaults())
				.authorizeHttpRequests(authz -> authz
						.requestMatchers("/auth/**").permitAll()
						.anyRequest().authenticated()
				)
				.oauth2Login(oauth2 -> oauth2
						.successHandler((request, response, authentication) -> {
							// OAuth2AuthenticationToken を取得
							OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;

							// OAuth2AuthorizedClient をロード
							OAuth2AuthorizedClient client = authorizedClientService
									.loadAuthorizedClient(
											oauth2Token.getAuthorizedClientRegistrationId(),
											oauth2Token.getName()
									);

							// DefaultOidcUserを取得してIDトークンを取得
							DefaultOidcUser oidcUser = (DefaultOidcUser) authentication.getPrincipal();
							String idToken = oidcUser.getIdToken().getTokenValue();

							// IDトークンの情報を表示
							System.out.println("ID Token: " + idToken);

							// 必要なカスタム処理（ログ、通知、DB登録など）を追加
							// 例えば、ユーザーのメールアドレスを取得して利用することができます
							String email = oidcUser.getEmail();
							System.out.println("User email: " + email);


							// 1.クッキーで送付する方法
							// HTTPS通信のみ有効（そうでない場合はXSSに遭うリスク大）
							// IDトークンをHTTPOnlyクッキーに設定
							Cookie idTokenCookie = new Cookie("idToken", idToken);
							idTokenCookie.setSecure(true); // HTTPSのみで送信
							idTokenCookie.setPath("/");
							idTokenCookie.setMaxAge(3600); // クッキーの有効期限（秒単位）

							// レスポンスにクッキーを追加
							response.addCookie(idTokenCookie);

							// 2.レスポンスボディで送付する方法
							response.setContentType("application/json");
							response.setCharacterEncoding("UTF-8");

							// JSON形式でトークン情報をレスポンスボディに書き込む
							String jsonResponse = String.format("{\"idToken\": \"%s\"}", idToken);
							response.getWriter().write(jsonResponse);

							// レスポンスのステータスを200 OKに設定
							response.setStatus(HttpServletResponse.SC_OK);

							// 必要に応じて追加処理
							System.out.println("ID Token stored in cookie.");
						})
				)
				.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
		return http.build();
	}
}
