package uz.delivery_system.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mobile.device.Device;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uz.delivery_system.component.EhCacheBean;
import uz.delivery_system.enums.UserRole;
import uz.delivery_system.security.JwtTokenUtil;
import uz.delivery_system.security.JwtUser;
import uz.delivery_system.security.dto.JwtAuthenticationRequest;
import uz.delivery_system.security.dto.JwtAuthenticationResponse;
import uz.delivery_system.utils.SecurityUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Api(value = "Authorization", description = "login/password")
@RestController
public class AuthenticationRestController {

    @Value("${jwt.header}")
    private String tokenHeader;

    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @Autowired
    private EhCacheBean ehCacheBean;
    @Autowired
    private UserDetailsService userDetailsService;
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @ApiOperation(value = "admin",notes = "раздел авторизации для мобильного приложения admin. Для авторизации в заголовке можно написать произвольный текст.")
    @RequestMapping(value = "${route.authentication.owner}", method = RequestMethod.POST,consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<JwtAuthenticationResponse> createAuthenticationMobileDriverToken(@RequestBody JwtAuthenticationRequest authenticationRequest,Device device,HttpServletResponse response) throws AuthenticationException, IOException {
        // Perform the security
        System.out.println(passwordEncoder.encode(authenticationRequest.getPassword()));
        final Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authenticationRequest.getUsername(),
                        authenticationRequest.getPassword()
                )
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        JwtUser jwtUser = SecurityUtils.getUserDetails();
        if (!jwtUser.getAuthorities().contains(new SimpleGrantedAuthority(UserRole.ADMIN.authority()))) {
            throw new UsernameNotFoundException(authenticationRequest.getUsername()+" user not access to the admin panel");
        }
        final String token = jwtTokenUtil.generateToken(jwtUser, device);
        ehCacheBean.putUserDetails(jwtUser);
        // Return the token
        System.out.println(token);
        return ResponseEntity.ok(new JwtAuthenticationResponse(token, jwtUser.getFullName(),UserRole.ADMIN));
    }

    @ApiOperation(value = "firm menegeri",notes = "часть логина для фирменного менеджера, можно написать произвольный текст для авторизации в заголовке.")
    @RequestMapping(value = "${route.authentication.firm}", method = RequestMethod.POST,consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<JwtAuthenticationResponse> createAuthenticationFirmAdmin(@RequestBody JwtAuthenticationRequest authenticationRequest,Device device,HttpServletResponse response) throws AuthenticationException, IOException {
        // Perform the security

        final Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authenticationRequest.getUsername(),
                        authenticationRequest.getPassword()
                )
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        JwtUser jwtUser = SecurityUtils.getUserDetails();
        if (!jwtUser.getAuthorities().contains(new SimpleGrantedAuthority(UserRole.FIRM_ADMIN.authority()))) {
            throw new UsernameNotFoundException(authenticationRequest.getUsername()+" user not access to the admin panel");
        }
        final String token = jwtTokenUtil.generateToken(jwtUser, device);
        ehCacheBean.putUserDetails(jwtUser);
        // Return the token
        return ResponseEntity.ok(new JwtAuthenticationResponse(token, jwtUser.getFullName(),SecurityUtils.getUserRole()));
    }

    @ApiOperation(value = "shops",notes = "раздел входа, зарезервированный для пользователя мобильного приложения, созданного для магазинов, " +
            "Для авторизации в заголовке можно написать произвольный текст.")
    @RequestMapping(value = "${route.authentication.shop}", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<JwtAuthenticationResponse> createAuthenticationWebToken(@RequestBody JwtAuthenticationRequest authenticationRequest, Device device, HttpServletResponse response) throws AuthenticationException, IOException {
        // Perform the security
        System.out.println(authenticationRequest.getPassword());
        final Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authenticationRequest.getUsername(),
                        authenticationRequest.getPassword()
                )
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        JwtUser jwtUser = SecurityUtils.getUserDetails();
        if (!jwtUser.getAuthorities().contains(new SimpleGrantedAuthority(UserRole.SHOP_MANAGER.authority()))){
            throw new UsernameNotFoundException(authenticationRequest.getUsername()+" user not access to the shop application");
        }
        final String token = jwtTokenUtil.generateToken(jwtUser, device);
        ehCacheBean.putUserDetails(jwtUser);
        // Return the token
        return ResponseEntity.ok(new JwtAuthenticationResponse(token,jwtUser.getFullName(),SecurityUtils.getUserRole()));
    }

    @ApiOperation(value = "weaving",notes = "Часть входа зарезервирована для поставщика, " +
            "Для авторизации в заголовке можно написать произвольный текст.")
    @RequestMapping(value = "${route.authentication.deliver}", method = RequestMethod.POST,consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<JwtAuthenticationResponse> createAuthenticationMobileClientToken(@RequestBody JwtAuthenticationRequest authenticationRequest,Device device,HttpServletResponse response) throws AuthenticationException, IOException {
        // Perform the security
        final Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authenticationRequest.getUsername(),
                        authenticationRequest.getPassword()
                )
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        JwtUser jwtUser = SecurityUtils.getUserDetails();
        if (!jwtUser.getAuthorities().contains(new SimpleGrantedAuthority(UserRole.FIRM_DELIVER.authority()))) {
            throw new UsernameNotFoundException(authenticationRequest.getUsername()+" user not access to the deliver system application");
        }
        final String token = jwtTokenUtil.generateToken(jwtUser, device);
        ehCacheBean.putUserDetails(jwtUser);
        // Return the token
        return ResponseEntity.ok(new JwtAuthenticationResponse(token));
    }

    @ApiOperation(value = "token update",notes = "Обновление старого токена")
    @RequestMapping(value = "${route.authentication.refresh}", method = RequestMethod.GET)
    public ResponseEntity<JwtAuthenticationResponse> refreshAndGetAuthenticationToken(HttpServletRequest request) {
        String token = request.getHeader(tokenHeader);
        String username = jwtTokenUtil.getUsernameFromToken(token);
        JwtUser user = (JwtUser) userDetailsService.loadUserByUsername(username);

        if (jwtTokenUtil.canTokenBeRefreshed(token, user.getLastPasswordResetDate())) {
            String refreshedToken = jwtTokenUtil.refreshToken(token);
            return ResponseEntity.ok(new JwtAuthenticationResponse(refreshedToken));
        } else {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @ApiOperation(value = "logout",notes = "Выход из системы")
    @RequestMapping(value = "${route.logout}", method = RequestMethod.GET)
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String token = request.getHeader(tokenHeader);
        if(!StringUtils.isEmpty(token)){
            String username = jwtTokenUtil.getUsernameFromToken(token);
            ehCacheBean.removeUser(username);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
