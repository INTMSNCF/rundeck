package rundeck.controllers

import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import rundeck.User
import rundeck.services.ApiService
import rundeck.services.FrameworkService
import spock.lang.Specification
import spock.lang.Unroll

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@TestFor(UserController)
@Mock([User])
class UserControllerSpec extends Specification{

    @Unroll
    def "get info same user"(){
        given:
        def userToSearch = 'admin'
        User u = new User(login: userToSearch)
        u.save()
        UserAndRolesAuthContext auth = Mock(UserAndRolesAuthContext){
            getUsername()>>userToSearch
        }
        controller.frameworkService=Mock(FrameworkService){
            1 * getAuthContextForSubject(_)>>auth
        }
        controller.apiService=Mock(ApiService){
            1 * requireApi(_,_) >> true
            0 * renderErrorXml(_,_) >> {HttpServletResponse response, Map error->
                response.status=error.status
                null
            }
        }
        when:
        request.method='GET'
        request.format='xml'
        def result=controller.apiUserData()

        then:
        response.status==200
    }
    @Unroll
    def "get info different user as admin"(){
        given:
        def userToSearch = 'user'
        User u = new User(login: userToSearch)
        u.save()
        UserAndRolesAuthContext auth = Mock(UserAndRolesAuthContext){
            getUsername()>>'admin'
        }
        controller.frameworkService=Mock(FrameworkService){
            1 * getAuthContextForSubject(_)>>auth
            1 * authorizeApplicationResourceAny(_,_,_) >> true
        }
        controller.apiService=Mock(ApiService){
            1 * requireApi(_,_) >> true
            0 * renderErrorXml(_,_) >> {HttpServletResponse response, Map error->
                response.status=error.status
                null
            }

        }
        when:
        request.method='GET'
        request.format='xml'
        params.username=userToSearch
        //request.content=('<contents>'+text+'</contents>').bytes
        def result=controller.apiUserData()

        then:
        response.status==200
    }
    @Unroll
    def "get info different user as non admin"(){
        given:
        def userToSearch = 'admin'
        User u = new User(login: userToSearch)
        u.save()
        UserAndRolesAuthContext auth = Mock(UserAndRolesAuthContext){
            getUsername()>>'user'
        }
        controller.frameworkService=Mock(FrameworkService){
            1 * getAuthContextForSubject(_)>>auth
            1 * authorizeApplicationResourceAny(_,_,_) >> false
        }
        controller.apiService=Mock(ApiService){
            1 * requireApi(_,_) >> true
            1 * renderErrorXml(_,_) >> {HttpServletResponse response, Map error->
                response.status=error.status
                null
            }

        }
        when:
        request.method='GET'
        request.format='xml'
        params.username=userToSearch
        def result=controller.apiUserData()

        then:
        response.status==HttpServletResponse.SC_FORBIDDEN
    }

    @Unroll
    def "get info from unexistent user"(){
        given:
        def userToSearch = 'user'
        User u = new User(login: 'anotheruser')
        u.save()
        UserAndRolesAuthContext auth = Mock(UserAndRolesAuthContext){
            getUsername()>>userToSearch
        }
        controller.frameworkService=Mock(FrameworkService){
            1 * getAuthContextForSubject(_)>>auth
            0 * authorizeApplicationResourceAny(_,_,_) >> false
        }
        controller.apiService=Mock(ApiService){
            1 * requireApi(_,_) >> true
            1 * renderErrorXml(_,_) >> {HttpServletResponse response, Map error->
                response.status=error.status
                null
            }

        }
        when:
        request.method='GET'
        request.format='xml'
        params.username=userToSearch
        def result=controller.apiUserData()

        then:
        response.status==HttpServletResponse.SC_NOT_FOUND
    }

    @Unroll
    def "modify info same user using xml"(){
        given:
        def userToSearch = 'admin'
        def email = 'test@test.com'
        def text = '<?xml version="1.0" encoding="UTF-8"?><user><email>'+email+'</email></user>'
        User u = new User(login: userToSearch)
        u.save()
        UserAndRolesAuthContext auth = Mock(UserAndRolesAuthContext){
            getUsername()>>userToSearch
        }
        controller.frameworkService=Mock(FrameworkService){
            1 * getAuthContextForSubject(_)>>auth
        }
        controller.apiService=Mock(ApiService){
            1 * requireApi(_,_) >> true
            0 * renderErrorXml(_,_) >> {HttpServletResponse response, Map error->
                response.status=error.status
                null
            }
            1 * parseJsonXmlWith(_,_,_) >> {HttpServletRequest request, HttpServletResponse response,
                Map<String, Closure> handlers ->
                handlers.xml(request.XML)
                true
            }
        }
        when:
        request.method='POST'
        request.format='xml'
        request.content=(text).bytes
        def result=controller.apiUserData()

        then:
        response.status==200
        User savedUser = User.findByLogin(userToSearch)
        savedUser.email == email
    }

    @Unroll
    def "modify info same user using json"(){
        given:
        def userToSearch = 'admin'
        def email = 'test@test.com'
        def text = '{email:\''+email+'\',firstName:\'The\', lastName:\'Admin\'}'
        User u = new User(login: userToSearch)
        u.save()
        UserAndRolesAuthContext auth = Mock(UserAndRolesAuthContext){
            getUsername()>>userToSearch
        }
        controller.frameworkService=Mock(FrameworkService){
            1 * getAuthContextForSubject(_)>>auth
        }
        controller.apiService=Mock(ApiService){
            1 * requireApi(_,_) >> true
            0 * renderErrorXml(_,_) >> {HttpServletResponse response, Map error->
                response.status=error.status
                null
            }
            1 * parseJsonXmlWith(_,_,_) >> {HttpServletRequest request, HttpServletResponse response,
                                            Map<String, Closure> handlers ->
                handlers.json(request.JSON)
                true
            }
        }
        when:
        request.method='POST'
        request.format='json'
        request.content=(text).bytes
        def result=controller.apiUserData()

        then:
        response.status==200
        User savedUser = User.findByLogin(userToSearch)
        savedUser.email == email
    }

}
