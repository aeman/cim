package com.crossoverjie.cim.route.controller;

import com.crossoverjie.cim.common.enums.StatusEnum;
import com.crossoverjie.cim.common.exception.CIMException;
import com.crossoverjie.cim.common.pojo.CIMUserInfo;
import com.crossoverjie.cim.common.pojo.RouteInfo;
import com.crossoverjie.cim.common.res.BaseResponse;
import com.crossoverjie.cim.common.res.NULLBody;
import com.crossoverjie.cim.common.route.algorithm.RouteHandle;
import com.crossoverjie.cim.common.util.RouteInfoParseUtil;
import com.crossoverjie.cim.route.api.RouteApi;
import com.crossoverjie.cim.route.api.vo.req.ChatReqVO;
import com.crossoverjie.cim.route.api.vo.req.LoginReqVO;
import com.crossoverjie.cim.route.api.vo.req.P2PReqVO;
import com.crossoverjie.cim.route.api.vo.req.RegisterInfoReqVO;
import com.crossoverjie.cim.route.api.vo.res.CIMServerResVO;
import com.crossoverjie.cim.route.api.vo.res.RegisterInfoResVO;
import com.crossoverjie.cim.route.cache.ServerCache;
import com.crossoverjie.cim.route.service.AccountService;
import com.crossoverjie.cim.route.service.CommonBizService;
import com.crossoverjie.cim.route.service.UserInfoCacheService;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Function:
 *
 * @author crossoverJie
 * Date: 22/05/2018 14:46
 * @since JDK 1.8
 */
@Controller
@RequestMapping("/")
public class RouteController implements RouteApi {
    private final static Logger logger = LoggerFactory.getLogger(RouteController.class);

    @Autowired
    private ServerCache serverCache;

    @Autowired
    private AccountService accountService;

    @Autowired
    private UserInfoCacheService userInfoCacheService;

    @Autowired
    private CommonBizService commonBizService;

    @Autowired
    private RouteHandle routeHandle;

    @ApiOperation("?????? API")
    @RequestMapping(value = "groupRoute", method = RequestMethod.POST)
    @ResponseBody()
    @Override
    public BaseResponse<NULLBody> groupRoute(@RequestBody ChatReqVO groupReqVO) throws Exception {
        BaseResponse<NULLBody> res = new BaseResponse();

        logger.info("msg=[{}]", groupReqVO.toString());

        //???????????????????????????
        Map<String, CIMServerResVO> serverResVOMap = accountService.loadRouteRelated();
        for (Map.Entry<String, CIMServerResVO> cimServerResVOEntry : serverResVOMap.entrySet()) {
            String idAndToken = cimServerResVOEntry.getKey();
            Long userId = Long.valueOf(idAndToken.split(":")[0]);
            String token = idAndToken.split(":")[1];
            CIMServerResVO cimServerResVO = cimServerResVOEntry.getValue();
            if (userId.equals(groupReqVO.getUserId())) {
                //???????????????
                CIMUserInfo cimUserInfo = userInfoCacheService.loadUserInfoByUserId(groupReqVO.getUserId());
                logger.warn("????????????????????? userId={}", cimUserInfo.toString());
                continue;
            }

            //????????????
            ChatReqVO chatVO = new ChatReqVO(userId, token, groupReqVO.getMsg());
            accountService.pushMsg(cimServerResVO, groupReqVO.getUserId(), chatVO);
        }

        res.setCode(StatusEnum.SUCCESS.getCode());
        res.setMessage(StatusEnum.SUCCESS.getMessage());
        return res;
    }


    /**
     * ????????????
     *
     * @param p2pRequest
     * @return
     */
    @ApiOperation("?????? API")
    @RequestMapping(value = "p2pRoute", method = RequestMethod.POST)
    @ResponseBody()
    @Override
    public BaseResponse<NULLBody> p2pRoute(@RequestBody P2PReqVO p2pRequest) throws Exception {
        BaseResponse<NULLBody> res = new BaseResponse();

        try {
            //???????????????????????????????????????
            List<CIMServerResVO> cimServerResVOs = accountService.loadRouteRelatedByUserId(p2pRequest.getReceiveUserId());

            //p2pRequest.getReceiveUserId()==>?????????????????? userID
            for (CIMServerResVO cimServerResVO : cimServerResVOs) {
                ChatReqVO chatVO = new ChatReqVO(p2pRequest.getReceiveUserId(), cimServerResVO.getToken(), p2pRequest.getMsg());
                accountService.pushMsg(cimServerResVO, p2pRequest.getUserId(), chatVO);
            }

            res.setCode(StatusEnum.SUCCESS.getCode());
            res.setMessage(StatusEnum.SUCCESS.getMessage());
        } catch (CIMException e) {
            res.setCode(e.getErrorCode());
            res.setMessage(e.getErrorMessage());
        }
        return res;
    }


    @ApiOperation("???????????????")
    @RequestMapping(value = "offLine", method = RequestMethod.POST)
    @ResponseBody()
    @Override
    public BaseResponse<NULLBody> offLine(@RequestBody ChatReqVO groupReqVO) throws Exception {
        BaseResponse<NULLBody> res = new BaseResponse();

        CIMUserInfo cimUserInfo = userInfoCacheService.loadUserInfoByUserId(groupReqVO.getUserId());
        logger.info("user [{}] offline!", cimUserInfo.toString());

        accountService.offLine(groupReqVO.getUserId(), groupReqVO.getToken());

        res.setCode(StatusEnum.SUCCESS.getCode());
        res.setMessage(StatusEnum.SUCCESS.getMessage());
        return res;
    }

    /**
     * ???????????? CIM server
     *
     * @return
     */
    @ApiOperation("????????????????????????")
    @RequestMapping(value = "login", method = RequestMethod.POST)
    @ResponseBody()
    @Override
    public BaseResponse<CIMServerResVO> login(@RequestBody LoginReqVO loginReqVO) throws Exception {
        BaseResponse<CIMServerResVO> res = new BaseResponse();

        // check server available
        String server = routeHandle.routeServer(serverCache.getServerList(),
                loginReqVO.getUserId().toString() + loginReqVO.getToken());

        logger.info("userName=[{}] route server info=[{}]", loginReqVO.getUserName(), server);

        RouteInfo routeInfo = RouteInfoParseUtil.parse(server);
        commonBizService.checkServerAvailable(routeInfo);

        //????????????
        StatusEnum status = accountService.login(loginReqVO);
        if (status == StatusEnum.SUCCESS) {

            //??????????????????
            accountService.saveRouteInfo(loginReqVO, server);

            CIMServerResVO vo = new CIMServerResVO(routeInfo);
            res.setDataBody(vo);

        }
        res.setCode(status.getCode());
        res.setMessage(status.getMessage());

        return res;
    }

    /**
     * ????????????
     *
     * @return
     */
    @ApiOperation("????????????")
    @RequestMapping(value = "registerAccount", method = RequestMethod.POST)
    @ResponseBody()
    @Override
    public BaseResponse<RegisterInfoResVO> registerAccount(@RequestBody RegisterInfoReqVO registerInfoReqVO) throws Exception {
        BaseResponse<RegisterInfoResVO> res = new BaseResponse();

        long userId = System.currentTimeMillis();
        RegisterInfoResVO info = new RegisterInfoResVO(userId, registerInfoReqVO.getUserName());
        info = accountService.register(info);

        res.setDataBody(info);
        res.setCode(StatusEnum.SUCCESS.getCode());
        res.setMessage(StatusEnum.SUCCESS.getMessage());
        return res;
    }

    /**
     * ????????????????????????
     *
     * @return
     */
    @ApiOperation("????????????????????????")
    @RequestMapping(value = "onlineUser", method = RequestMethod.POST)
    @ResponseBody()
    @Override
    public BaseResponse<Set<CIMUserInfo>> onlineUser() throws Exception {
        BaseResponse<Set<CIMUserInfo>> res = new BaseResponse();

        Set<CIMUserInfo> cimUserInfos = userInfoCacheService.onlineUser();
        res.setDataBody(cimUserInfos);
        res.setCode(StatusEnum.SUCCESS.getCode());
        res.setMessage(StatusEnum.SUCCESS.getMessage());
        return res;
    }

}
