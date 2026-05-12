import React from 'react'
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs'
import { createNativeStackNavigator } from '@react-navigation/native-stack'
import HomeScreen from '@/screens/HomeScreen'
import LiveRaceScreen from '@/screens/LiveRaceScreen'
import PredictScreen from '@/screens/PredictScreen'
import LeaguesScreen from '@/screens/LeaguesScreen'
import LeagueDetailScreen from '@/screens/LeagueDetailScreen'
import ProfileScreen from '@/screens/ProfileScreen'
import { colors } from '@/theme/tokens'

export type AppTabParamList = {
  HomeTab: undefined
  Predict: undefined
  LeaguesTab: undefined
  Profile: undefined
}

export type HomeStackParamList = {
  Home: undefined
  LiveRace: {
    raceId: string
    leagueId?: string
    raceNumber?: number
    rankedDriverCodes?: string[]
  }
}

export type LeagueStackParamList = {
  Leagues: undefined
  LeagueDetail: { leagueId: string; leagueName: string }
}

const Tab = createBottomTabNavigator<AppTabParamList>()
const HomeStack = createNativeStackNavigator<HomeStackParamList>()
const LeagueStack = createNativeStackNavigator<LeagueStackParamList>()

function HomeStackNavigator() {
  return (
    <HomeStack.Navigator screenOptions={{ headerShown: false }}>
      <HomeStack.Screen name="Home" component={HomeScreen} />
      <HomeStack.Screen
        name="LiveRace"
        component={LiveRaceScreen}
        options={{ headerShown: true, title: 'Live Race', headerStyle: { backgroundColor: colors.surface }, headerTintColor: colors.textPrimary }}
      />
    </HomeStack.Navigator>
  )
}

function LeaguesStackNavigator() {
  return (
    <LeagueStack.Navigator screenOptions={{ headerShown: false }}>
      <LeagueStack.Screen name="Leagues" component={LeaguesScreen} />
      <LeagueStack.Screen name="LeagueDetail" component={LeagueDetailScreen} />
    </LeagueStack.Navigator>
  )
}

export default function AppNavigator() {
  return (
    <Tab.Navigator
      screenOptions={{
        headerShown: false,
        tabBarStyle: { backgroundColor: colors.surface, borderTopColor: colors.border },
        tabBarActiveTintColor: colors.primary,
        tabBarInactiveTintColor: colors.textMuted,
      }}
    >
      <Tab.Screen name="HomeTab" component={HomeStackNavigator} options={{ tabBarLabel: 'Home' }} />
      <Tab.Screen name="Predict" component={PredictScreen} options={{ tabBarLabel: 'Predict' }} />
      <Tab.Screen name="LeaguesTab" component={LeaguesStackNavigator} options={{ tabBarLabel: 'Leagues' }} />
      <Tab.Screen name="Profile" component={ProfileScreen} options={{ tabBarLabel: 'Profile' }} />
    </Tab.Navigator>
  )
}
