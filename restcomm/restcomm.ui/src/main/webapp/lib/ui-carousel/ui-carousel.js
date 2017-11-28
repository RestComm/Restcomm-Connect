'use strict';

(function (angular) {
  // Create all modules and define dependencies to make sure they exist
  // and are loaded in the correct order to satisfy dependency injection
  // before all nested files are concatenated by Gulp

  // Config
  angular.module('ui.carousel.config', []).value('ui.carousel.config', {
    debug: true
  });

  // Modules
  angular.module('ui.carousel.providers', []);
  angular.module('ui.carousel.controllers', []);
  angular.module('ui.carousel.directives', []);
  angular.module('ui.carousel', ['ui.carousel.config', 'ui.carousel.directives', 'ui.carousel.controllers', 'ui.carousel.providers']);
})(angular);
'use strict';

/**
 * angular-ui-carousel
 * for example:
 * length = 8, show = 4, scroll = 3, current = 0
 *          ---------
 *          |       |
 *  |4|5|6|7|0|1|2|3|4|5|6|7|1|2|3|4
 *          |       |
 *          ---------
 * rectangle is visible for users
 */
angular.module('ui.carousel.controllers').controller('CarouselController', ['$scope', '$element', '$timeout', '$q', 'Carousel', '$window', function ($scope, $element, $timeout, $q, Carousel, $window) {
  var _this = this;

  /**
   * Initial carousel
   *
   * Mirgate to angularjs 1.6
   * @see https://docs.angularjs.org/guide/migration#commit-bcd0d4
   */
  this.$onInit = function () {
    _this.initOptions();
    _this.initRanges();
    _this.setProps();
    _this.setupInfinite();
  };

  /**
   * Init option based on directive config
   */
  this.initOptions = function () {
    _this.options = angular.extend({}, Carousel.getOptions());

    // TODO customize attribute from directive
    if (_this.initialSlide !== undefined) {
      _this.options.initialSlide = _this.initialSlide;
    }
    if (_this.fade !== undefined) {
      _this.options.fade = _this.fade;
    }
    if (_this.autoplay !== undefined) {
      _this.options.autoplay = _this.autoplay;
    }
    if (_this.autoplaySpeed !== undefined) {
      _this.options.autoplaySpeed = _this.autoplaySpeed;
    }
    if (_this.cssEase !== undefined) {
      _this.options.cssEase = _this.cssEase;
    }
    if (_this.speed !== undefined) {
      _this.options.speed = _this.speed;
    }
    if (_this.infinite !== undefined) {
      _this.options.infinite = _this.infinite;
    }
    if (_this.arrows !== undefined) {
      _this.options.arrows = _this.arrows;
    }
    if (_this.dots !== undefined) {
      _this.options.dots = _this.dots;
    }
    if (_this.visiblePrev !== undefined) {
      _this.options.visiblePrev = _this.visiblePrev;
    }
    if (_this.visibleNext !== undefined) {
      _this.options.visibleNext = _this.visibleNext;
    }

    // TODO write more options for fade mode
    // In fade mode we have to setting slides-to-show and slides-to-scroll
    // to 1 slide
    if (_this.options.fade) {
      _this.options.slidesToShow = 1;
      _this.options.slidesToScroll = 1;
    } else {
      if (_this.show) {
        _this.options.slidesToShow = _this.show;
      }
      if (_this.scroll) {
        _this.options.slidesToScroll = _this.scroll;
      }
    }
  };

  /**
   * init variables, slides, ..
   */
  this.initRanges = function () {
    if (!_this.slides) {
      _this.slides = [];
    }

    _this.isCarouselReady = false;
    _this.isTrackMoving = false;
    _this.track = $element.find('.track');
    _this.width = 1; // Fake width
    _this.currentSlide = _this.options.initialSlide;
    _this.trackStyle = {};
    _this.slideStyle = {};

    _this.isVisibleDots = false;
    _this.isVisiblePrev = _this.options.visiblePrev;
    _this.isVisibleNext = _this.options.visibleNext;

    _this.isClickablePrev = false;
    _this.isClickableNext = false;

    _this.animType = null;
    _this.transformType = null;
    _this.transitionType = null;
  };

  /**
   * Init UI and carousel track
   */
  this.initUI = function () {
    _this.width = $element[0].clientWidth;

    // Update track width first
    _this.initTrack();

    // Then item style
    $timeout(function () {
      _this.updateItemStyle();
    }, 200);
  };

  /**
   * update common style for each carousel item
   */
  this.updateItemStyle = function () {
    _this.itemWidth = _this.width / _this.options.slidesToShow;
    _this.slideStyle = {
      'width': _this.itemWidth + 'px'
    };
  };

  /**
   * init carousel track
   * also make Carousel is Ready
   */
  this.initTrack = function () {
    var itemWidth = _this.width / _this.options.slidesToShow;
    var trackWidth = itemWidth * _this.slidesInTrack.length;

    _this.trackStyle.width = trackWidth + 'px';

    _this.slideHandler(_this.currentSlide).finally(function () {
      _this.isCarouselReady = true;

      if (!_this.options.fade) {
        _this.refreshTrackStyle();
      }

      // onInit callback
      if (_this.onInit) {
        _this.onInit();
      }
    }).catch(function () {
      // Catch err
    });
  };

  /**
   * @see https://github.com/kenwheeler/slick/blob/master/slick/slick.js#L680
   *
   * Sync slide to place it should be
   * for example:
   * - 9 total, 3 show, 3 scroll, current 1
   *   => next index = 3 (previous index counted = 0)
   *
   * and scroll to next page:
   * - 6 total, 1 show, 1 scroll, current 0 => next index = 1
   * - 9 total, 3 show, 3 scroll, current 1 => next index = 3
   * - 9 total, 3 show, 3 scroll, current 3 => next index = 6
   * - 9 total, 3 show, 3 scroll, current 8 => next index = 3
   * - 8 total, 4 show, 3 scroll, current 1 => next index = 4
   */
  this.next = function () {
    if (!_this.isClickableNext) {
      return false;
    }

    var indexOffset = _this.getIndexOffset();
    var slideOffset = indexOffset === 0 ? _this.options.slidesToScroll : indexOffset;

    _this.slideHandler(_this.currentSlide + slideOffset).catch(function () {
      // Catch err
    });
  };

  /**
   * move to previous slide
   * same calculate with next
   * @see next function
   */
  this.prev = function () {
    if (!_this.isClickablePrev) {
      return false;
    }

    var indexOffset = _this.getIndexOffset();
    var slideOffset = indexOffset === 0 ? _this.options.slidesToScroll : _this.options.slidesToShow - indexOffset;

    _this.slideHandler(_this.currentSlide - slideOffset).catch(function () {
      // Catch err
    });
  };

  /**
   * Get index offset
   */
  this.getIndexOffset = function () {
    var scrollOffset = _this.slides.length % _this.options.slidesToScroll !== 0;
    var indexOffset = scrollOffset ? 0 : (_this.slides.length - _this.currentSlide) % _this.options.slidesToScroll;

    return indexOffset;
  };

  /**
   * move to page
   * @params int page
   * Page counter from 0 (start = 0)
   */
  this.movePage = function (page) {
    var target = _this.options.slidesToScroll * page;
    _this.slideHandler(target).catch(function () {
      // Catch err
    });
  };

  /**
   * hanlder carousel
   * @description move carousel to correct page
   *
   * @params int index
   */
  this.slideHandler = function (index) {
    // TODO prevent when slides not exists
    if (!_this.slides) {
      return $q.reject('Carousel not fully setup');
    }

    // TODO Prevent when track is moving
    if (_this.isTrackMoving) {
      return $q.reject('Track is moving');
    }

    var len = _this.slides.length;
    var show = _this.options.slidesToShow;

    if (len <= show) {
      _this.correctTrack();
      return $q.reject('Length of slides smaller than slides to show');
    }

    // We need target to destination
    // and a anim slide to translate track
    //
    // anim = animSlide (which we use to move)
    // target = targetSlide
    var anim = index;
    var target = null;

    if (anim < 0) {
      if (len % _this.options.slidesToScroll !== 0) {
        target = len - len % _this.options.slidesToScroll;
      } else {
        target = len + anim;
      }
    } else if (anim >= len) {
      if (len % _this.options.slidesToScroll !== 0) {
        target = 0;
      } else {
        target = anim - len;
      }
    } else {
      target = anim;
    }

    if (_this.onBeforeChange) {
      // @see https://docs.angularjs.org/guide/directive
      _this.onBeforeChange({ currentSlide: _this.currentSlide, target: target });
    }

    // Fade handler
    if (_this.options.fade) {
      _this.currentSlide = target;

      // XXX
      // afterChange method
      // fire after faded
      // Should be revised
      $timeout(function () {
        _this.autoplayTrack();

        if (_this.onAfterChange) {
          _this.onAfterChange({ currentSlide: _this.currentSlide });
        }
      }, _this.options.speed);
      return $q.when('Handler fade');
    }

    // No-fade handler
    var itemWidth = _this.width / _this.options.slidesToShow;
    var left = -1 * target * itemWidth;
    if (_this.options.infinite) {
      left = -1 * (anim + show) * itemWidth;
    }

    _this.isTrackMoving = true;
    return _this.moveTrack(left).then(function () {
      _this.isTrackMoving = false;
      _this.currentSlide = target;
      _this.autoplayTrack();

      if (target !== anim) {
        _this.correctTrack();
      }

      if (!_this.options.infinite) {
        if (_this.currentSlide === 0) {
          _this.isClickablePrev = false;
          _this.isClickableNext = true;
        } else if (_this.currentSlide === _this.slidesInTrack.length - _this.options.slidesToShow) {
          _this.isClickableNext = false;
          _this.isClickablePrev = true;
        } else {
          _this.isClickablePrev = true;
          _this.isClickableNext = true;
        }
      }

      // XXX
      // afterChange method
      // fire after 200ms wakeup and correct track
      // Should be revised
      $timeout(function () {
        if (_this.onAfterChange) {
          _this.onAfterChange({ currentSlide: _this.currentSlide });
        }
      }, 200);
    });
  };

  /**
   * moveTrack
   * move track to left position using css3 translate
   * for example left: -1000px
   */
  this.moveTrack = function (left) {
    var deferred = $q.defer();
    if (_this.options.vertical === false) {
      _this.trackStyle[_this.animType] = 'translate3d(' + left + 'px, 0px, 0px)';
    } else {
      _this.trackStyle[_this.animType] = 'translate3d(0px, ' + left + 'px, 0px)';
    }

    $timeout(function () {
      deferred.resolve('Track moved');
    }, _this.options.speed);

    return deferred.promise;
  };

  /**
   * correctTrack
   * @description correct track after move to animSlide we have to move track
   * to exactly its position
   */
  this.correctTrack = function () {
    if (_this.options.infinite) {
      (function () {
        var left = 0;
        if (_this.slides.length > _this.options.slidesToShow) {
          left = -1 * (_this.currentSlide + _this.options.slidesToShow) * _this.itemWidth;
        }

        // Move without anim
        _this.trackStyle[_this.transitionType] = _this.transformType + ' ' + 0 + 'ms ' + _this.options.cssEase;

        _this.isTrackMoving = true;
        $timeout(function () {
          _this.trackStyle[_this.animType] = 'translate3d(' + left + 'px, 0, 0px)';

          // Revert animation
          $timeout(function () {
            _this.refreshTrackStyle();
            _this.isTrackMoving = false;
          }, 200);
        });
      })();
    }
  };

  /**
   * Refresh track style
   */
  this.refreshTrackStyle = function () {
    _this.trackStyle[_this.transitionType] = _this.transformType + ' ' + _this.options.speed + 'ms ' + _this.options.cssEase;
  };

  /**
   * autoplay track
   * @description autoplay = true
   */
  this.autoplayTrack = function () {
    if (_this.options.autoplay) {
      if (_this.timeout) {
        $timeout.cancel(_this.timeout);
      }

      _this.timeout = $timeout(function () {
        _this.next();

        $timeout.cancel(_this.timeout);
        _this.timeout = null;
      }, _this.options.autoplaySpeed);
    }
  };

  this.getSlideStyle = function (index) {
    var style = _this.slideStyle;
    if (_this.options.fade) {
      var left = -1 * index * _this.itemWidth;
      var uniqueStyle = {
        position: 'relative',
        top: '0px',
        left: left + 'px',
        'z-index': index === _this.currentSlide ? 10 : 9,
        opacity: index === _this.currentSlide ? 1 : 0
      };

      if (index >= _this.currentSlide - 1 && index <= _this.currentSlide + 1) {
        uniqueStyle.transition = 'opacity 250ms linear';
      }

      style = angular.extend(style, uniqueStyle);
    }

    return style;
  };

  /**
   * setupInfinite
   * To make carouse infinite we need close number of slidesToShow elements to
   * previous elements and to after elements
   *
   * length = 8, show = 4, scroll = 3, current = 0
   *          ---------
   *          |       |
   *  |4|5|6|7|0|1|2|3|4|5|6|7|1|2|3|4
   *          |       |
   *          ---------
   */
  this.setupInfinite = function () {
    // Clone
    var len = _this.slides.length;
    var show = _this.options.slidesToShow;

    var tmpTrack = angular.copy(_this.slides);

    if (_this.options.infinite && _this.options.fade === false) {
      if (len > show) {
        var number = show;
        for (var i = 0; i < number; i++) {
          tmpTrack.push(angular.copy(_this.slides[i]));
        }
        for (var _i = len - 1; _i >= len - show; _i--) {
          tmpTrack.unshift(angular.copy(_this.slides[_i]));
        }
      }
    }

    _this.slidesInTrack = tmpTrack;
  };

  /**
   * get number of dosts
   *
   * @return Array
   */
  this.getDots = function () {
    if (!_this.slides) {
      return [];
    }

    var dots = Math.ceil(_this.slides.length / _this.options.slidesToScroll);

    var res = [];
    for (var i = 0; i < dots; i++) {
      res.push(i);
    }
    return res;
  };

  /**
   * set carousel property
   *
   * - animType
   * - transformType
   * - transitionType
   */
  this.setProps = function () {
    var bodyStyle = document.body.style;

    /* eslint-disable */
    if (bodyStyle.OTransform !== undefined) {
      _this.animType = 'OTransform';
      _this.transformType = '-o-transform';
      _this.transitionType = 'OTransition';
    }
    if (bodyStyle.MozTransform !== undefined) {
      _this.animType = 'MozTransform';
      _this.transformType = '-moz-transform';
      _this.transitionType = 'MozTransition';
    }
    if (bodyStyle.webkitTransform !== undefined) {
      _this.animType = 'webkitTransform';
      _this.transformType = '-webkit-transform';
      _this.transitionType = 'webkitTransition';
    }
    if (bodyStyle.msTransform !== undefined) {
      _this.animType = 'msTransform';
      _this.transformType = '-ms-transform';
      _this.transitionType = 'msTransition';
    }
    if (bodyStyle.transform !== undefined && _this.animType !== false) {
      _this.animType = 'transform';
      _this.transformType = 'transform';
      _this.transitionType = 'transition';
    }
    /* eslint-enable */

    _this.transformsEnabled = true;
  };

  /**
   * Refresh carousel
   */
  this.refreshCarousel = function () {
    if (_this.slides && _this.slides.length && _this.slides.length > _this.options.slidesToShow) {
      _this.isVisibleDots = true;
      _this.isVisiblePrev = true;
      _this.isVisibleNext = true;
      _this.isClickablePrev = true;
      _this.isClickableNext = true;
    } else {
      _this.isVisibleDots = false;
      _this.isVisiblePrev = _this.options.visiblePrev || false;
      _this.isVisibleNext = _this.options.visibleNext || false;
      _this.isClickablePrev = false;
      _this.isClickableNext = false;
    }

    // Re-init UI
    _this.initUI();
  };

  /**
   * refresh model
   */
  $scope.$watchCollection('ctrl.slides', function (slides) {
    if (!slides) {
      return;
    }

    // Init carousel
    if (_this.currentSlide > slides.length - 1) {
      _this.currentSlide = slides.length - 1;
    }

    _this.setupInfinite();
    _this.refreshCarousel();
  });

  /**
   * update when resize
   *
   * @see https://github.com/mihnsen/ui-carousel/issues/10
   * @author tarkant
   */
  angular.element($window).on('resize', this.refreshCarousel);

  /**
   * cleanup when done
   *
   * @see https://github.com/mihnsen/ui-carousel/issues/10
   * @author tarkant
   */
  $scope.$on('$destroy', function () {
    angular.element($window).off('resize');
  });

  // Prior to v1.5, we need to call `$onInit()` manually.
  // (Bindings will always be pre-assigned in these versions.)
  if (angular.version.major === 1 && angular.version.minor < 5) {
    this.$onInit();
  }
}]);
'use strict';

angular.module('ui.carousel.directives').directive('uiCarousel', ['$compile', '$templateCache', '$sce', function ($compile, $templateCache, $sce) {

  return { restrict: 'AE',
    bindToController: true,
    scope: {
      name: '=?',
      slides: '=',
      show: '=?slidesToShow',
      scroll: '=?slidesToScroll',
      classes: '@',
      fade: '=?',
      onChange: '=?',
      disableArrow: '=?',
      autoplay: '=?',
      autoplaySpeed: '=?',
      cssEase: '=?',
      speed: '=?',
      infinite: '=?',
      arrows: '=?',
      dots: '=?',
      initialSlide: '=?',
      visibleNext: '=?',
      visiblePrev: '=?',

      // Method
      onBeforeChange: '&',
      onAfterChange: '&',
      onInit: '&'
    },
    link: function link($scope, el) {
      var template = angular.element($templateCache.get('ui-carousel/carousel.template.html'));

      // dynamic injections to override the inner layers' components
      var injectComponentMap = {
        'carousel-item': '.carousel-item',
        'carousel-prev': '.carousel-prev',
        'carousel-next': '.carousel-next'
      };

      var templateInstance = template.clone();
      angular.forEach(injectComponentMap, function (innerSelector, outerSelector) {
        var outerElement = el[0].querySelector(outerSelector);
        if (outerElement) {
          angular.element(templateInstance[0].querySelector(innerSelector)).html(outerElement.innerHTML);
        }
      });

      var compiledElement = $compile(templateInstance)($scope);
      el.addClass('ui-carousel').html('').append(compiledElement);
    },


    controller: 'CarouselController',
    controllerAs: 'ctrl'
  };
}]);
'use strict';

angular.module('ui.carousel.providers').provider('Carousel', function () {
  var _this = this;

  this.options = {
    // Init like Slick carousel
    // XXX Should be revised
    arrows: true,
    autoplay: false,
    autoplaySpeed: 3000,
    cssEase: 'ease',
    dots: false,

    easing: 'linear',
    fade: false,
    infinite: true,
    initialSlide: 0,

    slidesToShow: 1,
    slidesToScroll: 1,
    speed: 500,

    visiblePrev: false,
    visibleNext: false,

    // Not available right now
    draggable: true,

    lazyLoad: 'ondemand',

    swipe: true,
    swipeToSlide: false,
    touchMove: true,

    vertical: false,
    verticalSwiping: false
  };
  this.$get = [function () {
    return {
      setOptions: function setOptions(options) {
        _this.options = angular.extend(_this.options, options);
      },
      getOptions: function getOptions() {
        return _this.options;
      }
    };
  }];
});
'use strict';

(function (module) {
  try {
    module = angular.module('ui.carousel');
  } catch (e) {
    module = angular.module('ui.carousel', []);
  }
  module.run(['$templateCache', function ($templateCache) {
    $templateCache.put('ui-carousel/carousel.template.html', '<div class="carousel-wrapper" ng-show="ctrl.isCarouselReady"><div class="track-wrapper"><div class="track" ng-style="ctrl.trackStyle"><div class="slide" ng-repeat="item in ctrl.slidesInTrack track by $index" ng-style="ctrl.getSlideStyle($index)"><div class="carousel-item"></div></div></div></div><div class="carousel-prev" ng-if="!ctrl.disableArrow" ng-show="ctrl.isVisiblePrev &amp;&amp; ctrl.options.arrows" ng-class="{\'carousel-disable\': !ctrl.isClickablePrev}" ng-click="ctrl.prev()"><button class="carousel-btn"><i class="ui-icon-prev"></i></button></div><div class="carousel-next" ng-if="!ctrl.disableArrow" ng-show="ctrl.isVisibleNext &amp;&amp; ctrl.options.arrows" ng-class="{\'carousel-disable\': !ctrl.isClickableNext}" ng-click="ctrl.next()"><button class="carousel-btn"><i class="ui-icon-next"></i></button></div><ul class="carousel-dots" ng-show="ctrl.isVisibleDots &amp;&amp; ctrl.options.dots"><li ng-repeat="dot in ctrl.getDots()" ng-class="{ \'carousel-active\': dot == ctrl.currentSlide/ctrl.options.slidesToScroll }" ng-click="ctrl.movePage(dot)"><button>{{ dot }}</button></li></ul></div>');
  }]);
})();